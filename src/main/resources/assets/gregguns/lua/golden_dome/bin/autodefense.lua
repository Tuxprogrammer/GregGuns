-- autodefense start [--emp] | stop | status : fire every reachable air-defense battery at each incoming missile.
-- With --emp the EMP tower is set to missiles-only and fired too. Every action is logged to /tmp/autodefense.log.
local gd = require("goldendome")
local event = require("event")
local shell = require("shell")

local args, options = shell.parse(...)
local state = _G.goldendome_autodefense

local function log(line)
  print(line)
  local f = io.open("/tmp/autodefense.log", "a")
  if f then
    f:write(line .. "\n")
    f:close()
  end
end

local function onIncoming(_, _, contactId)
  for _, battery in ipairs(gd.list("gg_air_defense")) do
    local ok, reason = battery.fireAt(contactId)
    log(string.format("incoming %s: battery %s %s", tostring(contactId), battery.address:sub(1, 8),
      ok and "fired" or ("refused: " .. tostring(reason))))
  end
  if _G.goldendome_autodefense and _G.goldendome_autodefense.emp then
    local tower = gd.first("gg_emp_tower")
    if tower and tower.isReady() then
      local ok, reason = tower.fire()
      log("emp tower " .. (ok and "fired" or ("refused: " .. tostring(reason))))
    end
  end
end

local command = args[1] or "status"
if command == "start" then
  if state then
    print("Already running.")
    return
  end
  local emp = options.emp and true or false
  if emp then
    local tower = gd.first("gg_emp_tower")
    if not tower then
      io.stderr:write("No EMP tower connected; starting without --emp.\n")
      emp = false
    else
      tower.setMode("missiles")
    end
  end
  for _, battery in ipairs(gd.list("gg_air_defense")) do
    battery.setAuto(false) -- this program decides, not the battery
  end
  _G.goldendome_autodefense = { id = event.listen("radar_incoming", onIncoming), emp = emp }
  log("autodefense started" .. (emp and " with the EMP tower" or ""))
elseif command == "stop" then
  if not state then
    print("Not running.")
    return
  end
  event.cancel(state.id)
  _G.goldendome_autodefense = nil
  log("autodefense stopped")
elseif command == "status" then
  print(state and "autodefense is running" or "autodefense is not running")
  print("batteries: " .. #gd.list("gg_air_defense") .. ", radars: " .. #gd.list("gg_radar"))
else
  print("Usage: autodefense start [--emp] | stop | status")
  return 1
end
