-- radarwatch : a live table of every radar contact; beeps on an incoming missile. Ctrl+C to stop.
local gd = require("goldendome")
local event = require("event")
local term = require("term")
local computer = require("computer")

local alarm = false
local id = event.listen("radar_incoming", function()
  alarm = true
end)

local function draw()
  term.clear()
  print("Golden Dome radar watch -- Ctrl+C to stop")
  local contacts = gd.contacts(false)
  if #contacts == 0 then
    print("No contacts.")
    return
  end
  table.sort(contacts, function(a, b) return (a.eta or 1e9) < (b.eta or 1e9) end)
  for _, c in ipairs(contacts) do
    print(gd.formatContact(c))
  end
end

local ok, err = pcall(function()
  while true do
    draw()
    if alarm then
      alarm = false
      computer.beep(1000, 0.3)
      computer.beep(1400, 0.3)
    end
    event.pull(2)
  end
end)
event.cancel(id)
if not ok and not tostring(err):find("interrupted") then
  io.stderr:write(tostring(err) .. "\n")
end
