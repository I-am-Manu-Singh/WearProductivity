import json
import shutil
from pathlib import Path
from pathlib import Path

# Script is located in wear/src/main/assets/repdb/
REPDB_DIR = Path(__file__).resolve().parent

JSON_PATH = REPDB_DIR / "exercises.json"
IMAGE_ROOT = REPDB_DIR / "images" / "exercises"
ASSETS_ROOT = REPDB_DIR.parent

# Ordered by style, then animation/static frame.
FRAME_ORDER = [
    "classic-start",
    "classic-peak",
    "classic-main",
    "flat-start",
    "flat-peak",
    "flat-main",
]

def main():
    if not JSON_PATH.is_file():
        raise FileNotFoundError(f"JSON file not found: {JSON_PATH}")

    if not IMAGE_ROOT.is_dir():
        raise FileNotFoundError(
            f"Consolidated image directory not found: {IMAGE_ROOT}"
        )

    with JSON_PATH.open("r", encoding="utf-8") as file:
        exercises = json.load(file)

    if not isinstance(exercises, list):
        raise ValueError("Expected exercises.json to contain a JSON array.")

    # Avoid overwriting an existing backup.
    backup = JSON_PATH.with_suffix(".json.bak")
    if not backup.exists():
        shutil.copy2(JSON_PATH, backup)

    updated = 0
    missing = []

    for exercise in exercises:
        exercise_id = exercise.get("id")
        if not exercise_id:
            print("WARNING: Skipping an exercise without an id.")
            continue

        exercise_dir = IMAGE_ROOT / exercise_id
        paths = []

        if exercise_dir.is_dir():
            for stem in FRAME_ORDER:
                # Preserve supported image extensions and only list real files.
                for extension in (".webp", ".png", ".jpg", ".jpeg"):
                    candidate = exercise_dir / f"{stem}{extension}"
                    if candidate.is_file():
                        paths.append(candidate.relative_to(ASSETS_ROOT).as_posix())
                        break

        old_images = exercise.get("images", [])
        if not isinstance(old_images, list):
            raise ValueError(
                f"Exercise {exercise_id!r} has a non-list 'images' field."
            )

        if paths:
            exercise["images"] = paths
            updated += 1
        else:
            exercise["images"] = []
            missing.append(exercise_id)

    # Validate serialization before writing the updated JSON.
    output = json.dumps(exercises, indent=2, ensure_ascii=False)
    json.loads(output)

    with JSON_PATH.open("w", encoding="utf-8") as file:
        file.write(output + "\n")

    print(f"Updated exercises with images: {updated}")
    print(f"Exercises without images: {len(missing)}")
    if missing:
        print("Missing images:", ", ".join(missing))
    print(f"Backup: {backup}")
    print(f"Updated: {JSON_PATH}")

if __name__ == "__main__":
    main()
