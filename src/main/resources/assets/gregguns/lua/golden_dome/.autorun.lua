-- Golden Dome autorun: put bin/ on the shell path and lib/ on the module path while the disk is mounted, and take
-- them off again when it is removed (the pattern OpenSecurity's disk uses). Arguments: the filesystem proxy.
local fs = require("filesystem")
local event = require("event")
local proxy = ...

local mount
for p, path in fs.mounts() do
  if proxy and p.address == proxy.address then
    mount = path
    break
  end
end
if not mount then
  return
end

local bin = fs.concat(mount, "bin")
local lib = fs.concat(mount, "lib/?.lua")
local path = os.getenv("PATH") or "/bin:/usr/bin:/home/bin:."
if not path:find(bin, 1, true) then
  os.setenv("PATH", path .. ":" .. bin)
end
if not package.path:find(lib, 1, true) then
  package.path = package.path .. ";" .. lib
end
print("Golden Dome ready: launch, radarwatch, autodefense, emp")

local address = proxy.address
local id
id = event.listen("component_removed", function(_, removed, kind)
  if kind == "filesystem" and removed == address then
    os.setenv("PATH", (os.getenv("PATH") or ""):gsub(":" .. bin:gsub("%p", "%%%0"), ""))
    package.path = package.path:gsub(";" .. lib:gsub("%p", "%%%0"), "")
    event.cancel(id)
  end
end)
