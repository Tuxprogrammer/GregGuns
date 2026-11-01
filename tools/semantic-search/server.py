"""MCP server (stdio, JSON-RPC 2.0) giving Claude Code semantic search over this repository. Standard library plus numpy."""

import json
import sys
import threading
import time
import traceback
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import semsearch as s  # noqa: E402

REFRESH_AFTER = 600

INSTRUCTIONS = (
    "Semantic search over GregGuns: the Techguns, GregGuns and ICBM specs (docs/techguns-spec, docs/gregguns-spec, "
    "docs/icbm-spec, docs/gregguns-icbm-spec with its DECISIONS log), design records, roadmaps, tool READMEs, Java "
    "sources, Python tools and the en_US.lang player text, split by heading or class member. Use semantic_search for "
    "conceptual questions when you do not know the identifier or file: why a design is the way it is, where a "
    "behavior lives, which spec sheet or decision covers a feature, whether something already exists. Use grep for "
    "exact names. Each hit is a "
    "path:line range; read the file before acting on it. The index follows the main working tree, so a worktree's "
    "uncommitted edits are not in it."
)

TOOLS = [
    {
        "name": "semantic_search",
        "description": (
            "Search the repository by meaning. Returns the best-matching chunks as path:start-end with a score and "
            "a snippet. Phrase the query as a question or description, e.g. 'where is gun recoil decided' "
            "or 'decision about silo hatch layout'."
        ),
        "inputSchema": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "What you are looking for, in natural language."},
                "k": {"type": "integer", "minimum": 1, "maximum": 25, "default": 8, "description": "Number of hits."},
                "kind": {"type": "string", "enum": ["any", "docs", "java", "python", "lang", "build"], "default": "any",
                         "description": "Restrict to Markdown docs, Java, Python tools, the .lang player text, or build and other files."},
                "path": {"type": "string", "description": "Only paths starting with this prefix, e.g. 'docs/design/'."},
            },
            "required": ["query"],
        },
    },
    {
        "name": "semantic_index_status",
        "description": "Shows the index's size, the commit it was built at and whether a refresh is running.",
        "inputSchema": {"type": "object", "properties": {}},
    },
    {
        "name": "semantic_reindex",
        "description": "Refreshes the index now, embedding only chunks that changed. Usually unnecessary: the server refreshes itself in the background.",
        "inputSchema": {"type": "object", "properties": {}},
    },
]


def log(msg):
    print("[semantic-search] " + msg, file=sys.stderr, flush=True)


class Service:
    def __init__(self, root, embedder, path):
        self.root, self.embedder, self.path = root, embedder, path
        self.index = s.load_index(path)
        self.lock = threading.Lock()
        self.refreshing = False
        self.last_refresh = 0.0
        self.last_error = None

    def refresh(self):
        with self.lock:
            if self.refreshing or self.embedder is None:
                return False
            self.refreshing, self.last_refresh = True, time.time()
        try:
            index = s.build(self.root, self.embedder, self.path, log)
            self.index = index or s.load_index(self.path) or self.index
            self.last_error = None
        except Exception as e:  # a failed refresh must not take the server down
            self.last_error = str(e)
            log("refresh failed: %s" % e)
        finally:
            self.refreshing = False
        return True

    def refresh_in_background(self):
        if not self.refreshing and time.time() - self.last_refresh > REFRESH_AFTER:
            threading.Thread(target=self.refresh, daemon=True).start()

    def status(self):
        lines = []
        if self.index:
            m = self.index["meta"]
            lines.append("Index: %d chunks from %d files, model %s" % (m["chunks"], m["files"], m["model"]))
            lines.append("Built at commit %s, %s" % (m["commit"][:10], time.strftime("%Y-%m-%d %H:%M", time.localtime(m["built_at"]))))
            head = s.head_commit(self.root)
            lines.append("Current HEAD %s (%s)" % (head[:10], "same" if head == m["commit"] else "differs; refresh pending or running"))
        else:
            lines.append("No index yet at %s." % self.path)
        lines.append("Refresh running: %s" % ("yes" if self.refreshing else "no"))
        if self.embedder is None:
            lines.append("OPENROUTER_API_KEY is not set (environment or .env at the repository root); search is unavailable.")
        if self.last_error:
            lines.append("Last refresh error: " + self.last_error)
        return "\n".join(lines)

    def call(self, name, args):
        if name == "semantic_index_status":
            return self.status(), False
        if name == "semantic_reindex":
            if not self.refresh():
                return "A refresh is already running or no API key is set.\n" + self.status(), True
            return self.status(), bool(self.last_error)
        if name == "semantic_search":
            query = (args.get("query") or "").strip()
            if not query:
                return "query is required", True
            if self.embedder is None:
                return self.status(), True
            if self.index is None:
                if not self.refreshing:
                    threading.Thread(target=self.refresh, daemon=True).start()
                return "The index is being built for the first time (a few minutes). Try again shortly, or use grep.", True
            self.refresh_in_background()
            k = max(1, min(25, int(args.get("k") or 8)))
            hits = s.search(self.index, self.embedder.query(query), k, args.get("path"), args.get("kind"))
            return s.format_hits(hits), False
        return "unknown tool: %s" % name, True


def handle(service, msg):
    """Returns the JSON-RPC response for one message, or None for notifications."""
    method, mid, params = msg.get("method"), msg.get("id"), msg.get("params") or {}
    if mid is None:
        return None
    if method == "initialize":
        result = {
            "protocolVersion": params.get("protocolVersion", "2025-06-18"),
            "capabilities": {"tools": {}},
            "serverInfo": {"name": "gregguns-semantic-search", "version": "1.0.0"},
            "instructions": INSTRUCTIONS,
        }
    elif method == "ping":
        result = {}
    elif method == "tools/list":
        result = {"tools": TOOLS}
    elif method == "tools/call":
        try:
            text, is_error = service.call(params.get("name"), params.get("arguments") or {})
        except Exception as e:
            log(traceback.format_exc())
            text, is_error = "error: %s" % e, True
        result = {"content": [{"type": "text", "text": text}], "isError": is_error}
    else:
        return {"jsonrpc": "2.0", "id": mid, "error": {"code": -32601, "message": "method not found: %s" % method}}
    return {"jsonrpc": "2.0", "id": mid, "result": result}


def main():
    root = s.tool_root()
    main_root = s.main_root(root)
    key = s.load_key(root, main_root)
    service = Service(main_root, s.OpenRouter(key) if key else None, s.index_path(main_root))
    if key:
        threading.Thread(target=service.refresh, daemon=True).start()
    out_lock = threading.Lock()
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            msg = json.loads(line)
        except ValueError:
            continue
        reply = handle(service, msg)
        if reply is not None:
            with out_lock:
                sys.stdout.write(json.dumps(reply) + "\n")
                sys.stdout.flush()


if __name__ == "__main__":
    main()
