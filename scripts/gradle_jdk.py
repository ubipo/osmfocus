#!/usr/bin/env python3
import argparse
import os
import glob
import xml.etree.ElementTree as ET
from typing import Dict, Optional


def get_android_studio_application_home_dir() -> Optional[str]:
    """Best-effort resolution of Android Studio's application home directory."""
    env_value = os.environ.get("APPLICATION_HOME_DIR")
    if env_value:
        return env_value

    if os.name == "posix" and os.uname().sysname == "Darwin":
        candidates = []
        for pattern in (
            "/Applications/Android Studio*.app/Contents",
            os.path.expanduser("~/Applications/Android Studio*.app/Contents"),
        ):
            candidates.extend(glob.glob(pattern))

        if candidates:
            return max(candidates, key=os.path.getmtime)

    return None


def expand_idea_macros(path: str) -> str:
    """Expand known IntelliJ/Android Studio path macros in a filesystem path."""
    expanded = path.replace("$USER_HOME$", os.path.expanduser("~"))

    if "$APPLICATION_HOME_DIR$" in expanded:
        app_home = get_android_studio_application_home_dir()
        if app_home:
            expanded = expanded.replace("$APPLICATION_HOME_DIR$", app_home)

    return expanded

def get_latest_android_studio_config() -> Optional[str]:
    """Find the most recently used Android Studio config directory that has the JDK table."""
    pattern = os.path.expanduser("~/Library/Application Support/Google/AndroidStudio*")
    candidates = [
        d for d in glob.glob(pattern)
        if os.path.exists(os.path.join(d, "options", "jdk.table.xml"))
    ]
    if not candidates:
        return None
    return max(candidates, key=os.path.getmtime)


def load_jdk_table(config_dir: str) -> Dict[str, str]:
    """Parse Android Studio's JDK table → name → full path."""
    xml_path = os.path.join(config_dir, "options", "jdk.table.xml")
    tree = ET.parse(xml_path)
    root = tree.getroot()

    jdks: Dict[str, str] = {}
    for jdk in root.findall(".//jdk"):
        name_el = jdk.find("name")
        home_el = jdk.find("homePath")
        if name_el is not None and home_el is not None:
            name = name_el.get("value")
            path = home_el.get("value")
            if name and path:
                jdks[name] = path
    return jdks


def get_gradle_jvm(project_dir: str = ".") -> Optional[str]:
    """Read the current gradleJvm setting from .idea/gradle.xml."""
    xml_path = os.path.join(project_dir, ".idea", "gradle.xml")
    if not os.path.exists(xml_path):
        return None
    tree = ET.parse(xml_path)
    root = tree.getroot()
    for option in root.findall(".//option"):
        if option.get("name") == "gradleJvm":
            return option.get("value")
    return None


def resolve_jdk_path(gradle_jvm: Optional[str], jdk_table: Dict[str, str]) -> Optional[str]:
    """Resolve the gradleJvm value to a real filesystem path."""
    if not gradle_jvm:
        return None

    # Special macro
    if gradle_jvm == "#JAVA_HOME":
        return os.environ.get("JAVA_HOME")

    # Full path already given
    if os.path.isabs(gradle_jvm):
        return gradle_jvm

    # Lookup in Android Studio's JDK table
    resolved = jdk_table.get(gradle_jvm, gradle_jvm)  # fallback to the raw value
    return expand_idea_macros(resolved)


def print_info(gradle_jvm: Optional[str], resolved: Optional[str], jdk_table: Dict[str, str]) -> None:
    print(f"Current gradleJvm setting: {gradle_jvm or '(not set in project)'}")
    print(f"Resolved JDK path          : {resolved or '(none)'}")
    print("\nAll JDKs in Android Studio's JDK table:")
    for name, path in sorted(jdk_table.items()):
        print(f"  • {name:<20} → {path}")


def print_setting(gradle_jvm: Optional[str]) -> None:
    print(gradle_jvm or "")


def print_jdk(resolved: Optional[str]) -> None:
    print(resolved or "")


def print_table(jdk_table: Dict[str, str]) -> None:
    for name, path in sorted(jdk_table.items()):
        print(f"{name}\t{path}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Inspect Gradle JVM/JDK resolution for this project.")
    subparsers = parser.add_subparsers(dest="command")

    subparsers.add_parser("info", help="Show gradleJvm setting, resolved JDK path, and JDK table.")
    subparsers.add_parser("setting", help="Print gradleJvm setting only.")
    subparsers.add_parser("jdk", help="Print resolved JDK path only.")
    subparsers.add_parser("table", help="Print JDK table only.")

    return parser.parse_args()


def main() -> int:
    args = parse_args()
    command = args.command or "info"

    config_dir = get_latest_android_studio_config()
    if not config_dir:
        print("❌ Could not find Android Studio config directory with jdk.table.xml")
        return 1

    jdk_table = load_jdk_table(config_dir)
    project_dir = "."
    gradle_jvm = get_gradle_jvm(project_dir)
    resolved = resolve_jdk_path(gradle_jvm, jdk_table)

    if command == "setting":
        print_setting(gradle_jvm)
    elif command == "jdk":
        print_jdk(resolved)
    elif command == "table":
        print_table(jdk_table)
    else:
        print_info(gradle_jvm, resolved, jdk_table)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
