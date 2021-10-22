# ☂️ Hodaka
This Discord bot is made for the Discord server [Ocean - Designer Network](https://discord.gg/NvWKYtGvGB).

## Setup
If you want to use the bot by yourself, add a `config.yaml` to the `resource` folder. Here is an example of how it should look like!
```yaml
# General
token: "jfA9f.gh98g9bwD?m.fg90gn0geghipo"
guildId: "8096706073663" # Id of discord server
prefixes:
  symbols:
    channels: "〢" # Channel prefixes
  emojis:
    unbans:
      accept: "🔓" # Prefix emoji for unbanned user channel
      deny: "🔒" # Prefix emoji for denies unban user channel
# Colours
colours:
  green: "#56f07f" # A green colour?
  red: "#eb4242" # And red I guess?
# Roles
roles:
  member: "863797639983360" # Default member role
  moderators: # Roles which have moderator permissions
    - "689730936908377453"
    - "38679936873963367"
# Text channels
channels:
  designSubmissions: "67936793679376363252"
  voice: "760306703760363" # Custom voice channel lobby id
  unbanRequest: "76923867334074" # Channel id to request unbans
  banLog: "8739236457894789" # Logging channel id for unbans
# Categories
categories:
  unbanRequests: "8963896370673" # Category id where all active unban channels sit
  achievedUnbanRequests: "98789679739670736" # Category id where done unban channels sit
designerRolePrefix: "🎨〢" # Prefix emoji of designer roles
submissionEvaluation: "2" # Days until to evaluate of the reactions
```
