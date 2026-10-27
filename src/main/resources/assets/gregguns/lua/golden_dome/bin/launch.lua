-- launch <x> <z> | launch --stick : set a target, show the missile, ask, then fire.
local gd = require("goldendome")
local shell = require("shell")

local args, options = shell.parse(...)

local function usage()
  print("Usage: launch <x> <z>   aim at a column")
  print("       launch --stick   take the target from a data stick in a Launch Control")
  print("       launch --list    list the launchers that are connected")
end

local launchers = gd.launchers()
if options.list then
  for i, l in ipairs(launchers) do
    print(i, l.type, l.address:sub(1, 8))
  end
  return
end
if #launchers == 0 then
  io.stderr:write("No silo or launch control connected (put an Adapter next to one).\n")
  return 1
end
local l = launchers[1]
if options.stick then
  -- Only a Launch Control reads a data stick; a silo answers no_stick.
  l = nil
  for _, candidate in ipairs(launchers) do
    if candidate.type == "gg_launch_control" then
      l = candidate
      break
    end
  end
  if not l then
    io.stderr:write("No launch control connected; a data stick needs one.\n")
    return 1
  end
end

if options.stick then
  local ok, reason = l.setTargetFromStick()
  if not ok then
    io.stderr:write("Could not read the stick: " .. tostring(reason) .. "\n")
    return 1
  end
else
  local x, z = gd.coords(args[1], args[2])
  if not x then
    usage()
    return 1
  end
  local ok, reason = l.setTarget(x, z)
  if not ok then
    io.stderr:write("Bad target: " .. tostring(reason) .. "\n")
    return 1
  end
end

local target = l.getTarget()
local missile = l.getMissile()
local fuel = l.getFuel()
print("Launcher: " .. l.type .. " " .. l.address:sub(1, 8))
print("Target:   " .. (target and (target.x .. ", " .. target.z) or "none"))
if missile then
  print(string.format("Missile:  tier %s, explosive %s, fill %s, fueled %s",
    tostring(missile.tier), tostring(missile.explosive), tostring(missile.fill), tostring(missile.fueled)))
else
  print("Missile:  none")
end
if fuel then
  print(string.format("Fuel:     %s %d / %d mB", tostring(fuel.fluid), fuel.amount or 0, fuel.required or 0))
end

local can, why = l.canLaunch()
if not can then
  io.stderr:write("Cannot launch: " .. tostring(why) .. "\n")
  return 1
end
if not gd.confirm("Launch now? This cannot be undone.") then
  print("Canceled.")
  return 0
end
local ok, reason = l.launch()
if ok then
  print("Launched.")
else
  io.stderr:write("Refused: " .. tostring(reason) .. "\n")
  return 1
end
