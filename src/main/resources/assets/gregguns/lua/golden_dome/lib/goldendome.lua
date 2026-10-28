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
  -- eta is in ticks (-1: not moving sideways); show seconds. Lua 5.3 refuses %d for a non-integral float, so every
  -- coordinate goes through math.floor.
  local eta = (c.eta and c.eta >= 0) and string.format("%4ds", math.floor(c.eta / 20)) or "   ? "
  return string.format("%s  T%s  at %6d %4d %6d  impact %6d %6d  eta %s",
    flag, tostring(c.tier), math.floor(c.x), math.floor(c.y), math.floor(c.z), math.floor(c.impactX or 0),
    math.floor(c.impactZ or 0), eta)
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

-- Like shell.parse (OpenOS' lib/shell.lua), but a negative number ("-48", "-3.5") is an argument, not a bunch of
-- one-letter options, so "launch -48 8512" works. "--" still ends the options. Returns args, options.
function gd.parse(...)
  local params = table.pack(...)
  local args, options = {}, {}
  local done = false
  for i = 1, params.n do
    local p = params[i]
    if done or type(p) ~= "string" or p == "-" or tonumber(p) then
      args[#args + 1] = p
    elseif p == "--" then
      done = true
    elseif p:sub(1, 2) == "--" then
      local key, value = p:match("%-%-(.-)=(.*)")
      if not key then
        key, value = p:sub(3), true
      end
      options[key] = value
    elseif p:sub(1, 1) == "-" then
      for j = 2, #p do
        options[p:sub(j, j)] = true
      end
    else
      args[#args + 1] = p
    end
  end
  return args, options
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
