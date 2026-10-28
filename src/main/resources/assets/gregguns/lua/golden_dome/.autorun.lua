-- Golden Dome autorun: put bin/ on the shell path and lib/ on the module path while the disk is mounted, and take
-- them off again when it is removed. Arguments: the filesystem proxy.
--
-- OpenOS runs this file as a process of its own, and every process keeps its own copy of the environment variables
-- (sh.lua calls shell.prime()), so a plain os.setenv("PATH", ...) would die with this process and the player's shell
-- would never see it. OpenOS offers no mount hook for PATH (a loot disk's usr/bin is meant for the `install`
-- command), so we edit the variable tables of the processes above us, which include the shell that took the
-- component_added event.
-- Source: OpenComputers 1.12.64-GTNH, openos/boot/90_filesystem.lua (runs the autorun through shell.execute),
-- openos/lib/shell.lua (shell.prime, shell.resolve reads PATH of the current process) and openos/lib/process.lua
-- (process.info(level) walks up the parents):
-- https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/resources/assets/opencomputers/loot/openos/boot/90_filesystem.lua
-- https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/resources/assets/opencomputers/loot/openos/lib/shell.lua
-- https://github.com/GTNewHorizons/OpenComputers/blob/1e4559ff5f2443695cb28c7cdc9fba87219a862b/src/main/resources/assets/opencomputers/loot/openos/lib/process.lua
local fs = require("filesystem")
local event = require("event")
local process = require("process")
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
local DEFAULT_PATH = "/bin:/usr/bin:/home/bin:."

-- Calls change(oldPath) -> newPath on the PATH of this process and of every process above it that has its own
-- variable table, so the interactive shell (and anything it starts from now on) sees the result.
local function editPath(change)
  local seen = {}
  local level = 1
  local info = process.info(level)
  while info do
    local vars = info.data and rawget(info.data, "vars")
    if vars and not seen[vars] then
      seen[vars] = true
      vars.PATH = change(vars.PATH or DEFAULT_PATH)
    end
    level = level + 1
    info = process.info(level)
  end
  -- A process without a table of its own shares its parent's, which the loop above has already reached through
  -- the parent; this call covers the plain case where nothing above has one.
  if not next(seen) then
    os.setenv("PATH", change(os.getenv("PATH") or DEFAULT_PATH))
  end
end

editPath(function(path)
  if path:find(bin, 1, true) then
    return path
  end
  return path .. ":" .. bin
end)
if not package.path:find(lib, 1, true) then
  package.path = package.path .. ";" .. lib
end
print("Golden Dome ready: launch, radarwatch, autodefense, emp")

local address = proxy.address
local id
id = event.listen("component_removed", function(_, removed, kind)
  if kind == "filesystem" and removed == address then
    editPath(function(path)
      return (path:gsub(":" .. bin:gsub("%p", "%%%0"), ""))
    end)
    package.path = package.path:gsub(";" .. lib:gsub("%p", "%%%0"), "")
    event.cancel(id)
  end
end)
