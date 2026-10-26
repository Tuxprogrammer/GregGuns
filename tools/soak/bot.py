#!/usr/bin/env python3
"""Minimal headless Minecraft 1.7.10 (protocol 5) + FML bot for soak tests. stdlib only.
usage: bot.py <name> <host> <port> <seconds> [cmdfile]
cmdfile: lines 'delay_seconds<TAB>chat line' sent as chat (e.g. /tp ...). Prints a stats line every 30 s."""
import socket, struct, sys, time, json, threading, zlib, os

def vi(n):
    out = bytearray()
    n &= 0xFFFFFFFF
    while True:
        b = n & 0x7F; n >>= 7
        if n: out.append(b | 0x80)
        else: out.append(b); return bytes(out)
def vs(s):
    b = s.encode(); return vi(len(b)) + b
class Buf:
    def __init__(s, d): s.d = d; s.i = 0
    def byte(s): v = s.d[s.i]; s.i += 1; return v
    def varint(s):
        r = 0; sh = 0
        while True:
            b = s.byte(); r |= (b & 0x7F) << sh; sh += 7
            if not b & 0x80: return r
    def string(s):
        n = s.varint(); v = s.d[s.i:s.i+n].decode('utf8', 'replace'); s.i += n; return v
    def take(s, n): v = s.d[s.i:s.i+n]; s.i += n; return v
    def unpack(s, f):
        sz = struct.calcsize(f); v = struct.unpack(f, s.d[s.i:s.i+sz]); s.i += sz; return v

class Conn:
    def __init__(s, host, port):
        s.sock = socket.create_connection((host, port), timeout=60)
        s.lock = threading.Lock()
        s.rbuf = b''
    def send(s, pid, payload=b''):
        body = vi(pid) + payload
        with s.lock: s.sock.sendall(vi(len(body)) + body)
    def _read(s, n):
        while len(s.rbuf) < n:
            c = s.sock.recv(1 << 16)
            if not c: raise EOFError('closed')
            s.rbuf += c
        v, s.rbuf = s.rbuf[:n], s.rbuf[n:]; return v
    def recv(s):
        r = 0; sh = 0
        while True:
            b = s._read(1)[0]; r |= (b & 0x7F) << sh; sh += 7
            if not b & 0x80: break
        body = Buf(s._read(r)); pid = body.varint(); return pid, body

def handshake(c, host, port, state, fml=True):
    c.send(0, vi(5) + vs(host + ('\0FML\0' if fml else '')) + struct.pack('>H', port) + vi(state))

def status(host, port):
    c = Conn(host, port); handshake(c, host, port, 1, fml=False); c.send(0)
    pid, b = c.recv(); return json.loads(b.string())

def plugin(c, chan, data):
    c.send(0x17, vs(chan) + struct.pack('>h', len(data)) + data)

def run(name, host, port, seconds, cmdfile):
    st = status(host, port)
    mods = st.get('modinfo', {}).get('modList', [])
    c = Conn(host, port); handshake(c, host, port, 2); c.send(0, vs(name))
    stats = dict(chunks=0, chunkbytes=0, bulk=0, packets=0, chat=0, tp=0)
    t0 = time.time(); joined = False
    cmds = []
    if cmdfile and os.path.exists(cmdfile):
        for ln in open(cmdfile):
            ln = ln.rstrip('\n')
            if ln and not ln.startswith('#'):
                d, _, txt = ln.partition('\t'); cmds.append((float(d), txt))
    cmds.sort(); ci = 0; last = t0; lastpos = None
    log = lambda m: print('[%s %7.1f] %s' % (name, time.time() - t0, m), flush=True)
    c.sock.settimeout(5)
    while time.time() - t0 < seconds:
        now = time.time() - t0
        while joined and ci < len(cmds) and cmds[ci][0] <= now:
            c.send(0x01, vs(cmds[ci][1])); log('sent ' + cmds[ci][1]); ci += 1
        if time.time() - last > 30:
            last = time.time(); log('stats %s' % stats)
        try: pid, b = c.recv()
        except socket.timeout: continue
        except EOFError: log('connection closed by server'); break
        stats['packets'] += 1
        if not joined and pid == 0x02 and b.d: pass
        if pid == 0x00 and not joined and stats['packets'] == 1:
            log('login-state disconnect: ' + b.string()); break
        if pid == 0x3F:
            chan = b.string(); n = b.unpack('>h')[0]; data = b.take(n)
            if chan == 'FML|HS':
                disc = data[0]
                if disc == 0:
                    plugin(c, 'FML|HS', bytes([1, 2]))
                    ml = bytes([2]) + vi(len(mods)) + b''.join(vs(m['modid']) + vs(m['version']) for m in mods)
                    plugin(c, 'FML|HS', ml); log('FML ServerHello, sent ModList of %d' % len(mods))
                elif disc == 2: plugin(c, 'FML|HS', bytes([255, 2]))
                elif disc == 3:
                    if data[1] == 0: plugin(c, 'FML|HS', bytes([255, 3]))
                elif disc == 255:
                    ph = data[1]
                    plugin(c, 'FML|HS', bytes([255, ph + 1]) if ph in (2, 3, 4) else bytes([255, ph]))
                    if ph == 3: pass
                    log('FML ack phase %d' % ph)
                elif disc == 254: log('FML reset')
            continue
        if pid == 0x00: c.send(0x00, struct.pack('>i', b.unpack('>i')[0]))
        elif pid == 0x01 and not joined:
            joined = True; eid, gm, dim = b.unpack('>iBb'); log('joined eid=%d dim=%d' % (eid, dim))
        elif pid == 0x02:
            stats['chat'] += 1
            try:
                txt = json.loads(b.string()).get('text', '')
            except Exception: txt = ''
            if txt: log('chat: ' + txt[:200])
        elif pid == 0x08:
            x, y, z, yaw, pitch, g = b.unpack('>dddff?'); stats['tp'] += 1
            c.send(0x06, struct.pack('>ddddff?', x, y - 1.62, y, z, yaw, pitch, True)); lastpos = (x, y, z)
        elif pid == 0x21: stats['chunks'] += 1; stats['chunkbytes'] += len(b.d)
        elif pid == 0x26: stats['bulk'] += 1; stats['chunkbytes'] += len(b.d)
        elif pid == 0x40:
            log('kicked: ' + b.string()); break
    log('final %s lastpos=%s' % (stats, lastpos))

if __name__ == '__main__':
    n, h, p, s = sys.argv[1], sys.argv[2], int(sys.argv[3]), float(sys.argv[4])
    run(n, h, p, s, sys.argv[5] if len(sys.argv) > 5 else None)
