# Live Trail map-pin notes

Biomate now supports two Trail Moment placement modes during an active adventure:

1. **Add moment** uses the phone's current GPS fix.
2. **Pin note** lets the user deliberately choose a point on Google Maps. Tap **Pin note**
   and then tap the map, or long-press the map directly. A note/category dialog opens, and
   the saved Trail Moment is persisted at the selected coordinates.

The selected pin appears in the existing Trail Moment stream immediately after saving and
continues to use the existing category filters, persistence, ownership and upvote logic.

## Files changed

- `app/src/main/java/com/example/ui/screens/OnTrailScreen.kt`
- `app/src/main/java/com/example/ui/viewmodel/OnTrailViewModel.kt`
- Trail Moment repository/domain comments updated to reflect both placement modes.

## Test locally

```powershell
.\verify_and_build.ps1
.\install_debug.ps1
```

Then open a trail, start the adventure, tap **Pin note**, tap the desired location, enter
a note, and tap **Share**.
