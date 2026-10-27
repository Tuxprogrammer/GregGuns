-- Golden Dome shared library (GregGuns ICBM). Players may require("goldendome") in their own scripts.
-- Components: gg_silo, gg_launch_control, gg_radar, gg_air_defense, gg_emp_tower (reached through an Adapter).
local component = require("component")
local event = require("event")

local gd = {}

-- Every proxy of a component type, in address order.
function gd.list(kind)
  local out = {}
  for address in component.list(kind) do
    out[#out + 1] = component.proxy(address)
  end
  table.sort(out, function(a, b) return a.address < b.address end)
  return out
end

-- The first proxy of a type, or nil and a message.
function gd.first(kind)
  local all = gd.list(kind)
  if #all == 0 then
    return nil, "no " .. kind .. " connected (put an Adapter next to the machine)"
  end
  return all[1]
end

-- Silos first, then launch controls.
function gd.launchers()
  local out = gd.list("gg_silo")
  for _, p in ipairs(gd.list("gg_launch_control")) do
    out[#out + 1] = p
  end
  return out
end

-- Asks a yes/no question; anything but y or yes is no.
function gd.confirm(question)
  io.write(question .. " [y/N] ")
  local answer = io.read()
  if not answer then
    return false
  end
  answer = answer:lower()
  return answer == "y" or answer == "yes"
end

-- One line for a radar contact record.
function gd.formatContact(c)
  local flag = c.incoming and "INCOMING" or "passing "
  local eta = c.eta and string.format("%4ds", math.floor(c.eta)) or "   ? "
  return string.format("%s  T%s  at %6d %4d %6d  impact %6d %6d  eta %s",
    flag, tostring(c.tier), c.x, c.y, c.z, c.impactX or 0, c.impactZ or 0, eta)
end

-- Every contact of every reachable radar, with the radar's short address.
function gd.contacts(incomingOnly)
  local out = {}
  for _, radar in ipairs(gd.list("gg_radar")) do
    local list = incomingOnly and radar.getIncoming() or radar.getContacts()
    for _, c in ipairs(list or {}) do
      c.radar = radar.address:sub(1, 8)
      out[#out + 1] = c
    end
  end
  return out
end

-- Waits for a signal by name; returns its arguments after the name, or nil on timeout.
function gd.waitSignal(name, timeout)
  local result = table.pack(event.pull(timeout, name))
  if result.n == 0 or result[1] == nil then
    return nil
  end
  return table.unpack(result, 2, result.n)
end

-- Splits "x z" arguments into numbers; returns nil if they are not numbers.
function gd.coords(a, b)
  local x, z = tonumber(a), tonumber(b)
  if not x or not z then
    return nil
  end
  return math.floor(x), math.floor(z)
end

return gd
