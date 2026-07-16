#!/usr/bin/env python3

import unittest

from build_repo import validate_unique_slugs
from changed_modules import select_modules


class SelectModulesTest(unittest.TestCase):
    modules = {
        "src/all/rezka": "rezka",
        "src/en/gutenberg": "gutenberg",
        "src/en/novelbuddy": "novelbuddy",
    }

    def test_selects_only_changed_extension(self) -> None:
        selected, deleted = select_modules(
            self.modules,
            [("M", "src/en/gutenberg/src/GutenbergSource.kt")],
        )

        self.assertEqual(selected, {"src/en/gutenberg"})
        self.assertEqual(deleted, set())

    def test_selects_only_new_extension(self) -> None:
        selected, deleted = select_modules(
            self.modules,
            [
                ("A", "src/en/novelbuddy/build.gradle.kts"),
                ("A", "src/en/novelbuddy/src/NovelBuddySource.kt"),
            ],
        )

        self.assertEqual(selected, {"src/en/novelbuddy"})
        self.assertEqual(deleted, set())

    def test_reports_deleted_extension_without_builds(self) -> None:
        selected, deleted = select_modules(
            self.modules,
            [("D", "src/en/novelarrow/build.gradle.kts")],
        )

        self.assertEqual(selected, set())
        self.assertEqual(deleted, {"novelarrow"})

    def test_rebuilds_all_for_shared_build_change(self) -> None:
        for path in ("gradle/libs.versions.toml", "settings.gradle.kts"):
            with self.subTest(path=path):
                selected, deleted = select_modules(
                    self.modules,
                    [("M", path)],
                )

                self.assertEqual(selected, set(self.modules))
                self.assertEqual(deleted, set())

    def test_rebuilds_all_without_published_baseline(self) -> None:
        selected, deleted = select_modules(self.modules, [], missing_base=True)

        self.assertEqual(selected, set(self.modules))
        self.assertEqual(deleted, set())


class ValidateUniqueSlugsTest(unittest.TestCase):
    def test_accepts_unique_slugs(self) -> None:
        validate_unique_slugs(
            [
                {"path": "src/all/rezka", "slug": "rezka"},
                {"path": "src/en/gutenberg", "slug": "gutenberg"},
            ],
        )

    def test_rejects_duplicate_slugs(self) -> None:
        with self.assertRaisesRegex(
            SystemExit,
            r"duplicate: src/en/duplicate, src/es/duplicate",
        ):
            validate_unique_slugs(
                [
                    {"path": "src/en/duplicate", "slug": "duplicate"},
                    {"path": "src/es/duplicate", "slug": "duplicate"},
                ],
            )


if __name__ == "__main__":
    unittest.main()
