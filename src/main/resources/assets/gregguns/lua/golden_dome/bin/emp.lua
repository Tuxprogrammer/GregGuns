-- emp status | emp mode <all|missiles|machines> | emp radius <r> | emp fire
local gd = require("goldendome")
local shell = require("shell")

local args = shell.parse(...)
local tower, why = gd.first("gg_emp_tower")
if not tower then
  io.stderr:write(why .. "\n")
  return 1
end

local command = args[1] or "status"
if command == "status" then
  local info = tower.getInfo()
  print("mode:     " .. tower.getMode())
  print("radius:   " .. tower.getRadius() .. " blocks (" .. info.minRadius .. " to " .. info.maxRadius .. ")")
  print("ready:    " .. tostring(tower.isReady()) .. ", cooldown " .. tower.getCooldown() .. " ticks")
  print("devices per pulse at most: " .. tostring(info.maxDevices))
elseif command == "mode" then
  local ok, reason = tower.setMode(args[2] or "")
  if not ok then
    io.stderr:write("Bad mode: " .. tostring(reason) .. "\n")
    return 1
  end
  print("mode: " .. tower.getMode())
elseif command == "radius" then
  local r = tonumber(args[2])
  if not r then
    io.stderr:write("Usage: emp radius <blocks>\n")
    return 1
  end
  print("radius: " .. tower.setRadius(r))
elseif command == "fire" then
  print("Mode " .. tower.getMode() .. ", radius " .. tower.getRadius() .. ".")
  if not tower.isReady() then
    io.stderr:write("The tower is not ready.\n")
    return 1
  end
  if not gd.confirm("Fire the EMP pulse? Machines in range may be damaged.") then
    print("Cancelled.")
    return 0
  end
  local ok, reason = tower.fire()
  if ok then
    print("Fired.")
  else
    io.stderr:write("Refused: " .. tostring(reason) .. "\n")
    return 1
  end
else
  print("Usage: emp status | mode <all|missiles|machines> | radius <r> | fire")
  return 1
end
