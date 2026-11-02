# HexGhoul: rules for every session

## RULE 1 (the owner's, for ALL sessions, no exceptions): stop at 97% usage

> "as soon as the session reaches 97% in ANY area, 5 hour or weekly instantly stop, save, produce handoff and prompt. no exceptions."

**Check the usage** before starting any piece of work, after every full build, and every few tool calls once either window is past 90%:

1. `mcp__claude-code-remote__get_session` (no arguments) gives this session's id (`ccr.id`).
2. `mcp__claude-code-remote__list_events` with that `session_id`, `kinds: ["rate_limit_event"]`, `limit: 100`: the newest events come back
   last; in each, `internal_anthropic_catchall.rate_limit_info.unifiedWindows` has `five_hour.utilization` and `seven_day.utilization`
   (0..1) and their `resetsAt` (unix seconds). Use the latest event. (`get_session`'s `external_metadata.rate_limit_info` shows only the
   window that is closest to its limit, without a number.)

**At 0.97 or more in either window**, at once, and nothing else first:

1. **Stop** what you are doing. Start no new step, no new build.
2. **Save**: commit everything that is changed and push to the session's branch. A version not yet built is committed as
   `1.3.xx WIP (not built): ...` (the build-before-commit rule gives way to this one), and the handoff says so.
3. **Handoff**: write `docs/handoff/next-ai-<last version>.md` (what is done, what is half done and where, what is left in order, the
   owner's words for what is left, anything untested or uncertain) and update the top of `docs/handoff/companions-handoff.md`. Commit
   and push it.
4. **Prompt**: give the owner, in the reply, a ready-to-paste prompt for the next session (branch, which handoff to read first, what to
   continue), and say which window hit 97% and when it resets.

If the tools to read the usage are missing, say so to the owner at the start and ask how they want it watched.

## Then

Read the newest `docs/handoff/next-ai-*.md` first: it holds the house rules (versions, builds, banned names, sounds, the API allowlist,
never altering the owner's files) and what to do next. Answer the owner in English.
