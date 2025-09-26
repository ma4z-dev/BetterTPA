# BetterTPA (DonutSMP-Style)

BetterTPA is a lightweight teleport request plugin for Minecraft **1.21+** servers.  
It works just like the TPA system on **DonutSMP** – clean menus, simple messages, and a teleport countdown.

---

## ⚡ Commands
| Command | Description |
|---------|-------------|
| `/tpa <player>` | Request to teleport to another player |
| `/tpahere <player>` | Request another player to teleport to you |
| `/tpaccept` | Accept or deny a teleport request (opens menu) |
| `/tpcancel` | Cancel your pending teleport request |
| `/better-tpa reload` | Reloads config (admin only) |

---

## 🎮 How It Works
1. **Send a request**  
   - `/tpa Steve` → asks Steve if you can teleport to him.  
   - `/tpahere Steve` → asks Steve to teleport to you.  

2. **Accept or deny**  
   - Steve runs `/tpaccept` → a menu pops up with `[ACCEPT]` or `[DENY]`.  

3. **Countdown**  
   - If Steve accepts, you see a hotbar countdown:  
     ```
     Teleporting in [5], [4], [3], [2], [1]...
     ```  

4. **Teleport**  
   - At 0, the teleport happens instantly.  

---

## ⚙️ Setup
1. Download **BetterTPA.jar**  
2. Drop it in your server’s `plugins/` folder  
3. Restart your server  
4. Edit `config.yml` (in `plugins/BetterTPA/`) if you want to customize messages or timings  
5. Use `/better-tpa reload` to apply changes without restarting  

---

## 🔑 Permissions
- `bettertpa.reload` → required for `/better-tpa reload` (default: OP)  
- All other commands are usable by everyone  

---

## 📌 Notes
- Works out of the box, no extra setup.  
- Requests auto-expire after the timeout you set in `config.yml`.  
- Overrides any existing `/tpa` commands from other plugins.  

---

## ✅ Just Like DonutSMP
This plugin was designed to feel exactly like **DonutSMP’s TPA system**:  
- Clean `[ACCEPT]` & `[DENY]` menu  
- No confusing menus or spam  
- Countdown teleport messages in the hotbar  
