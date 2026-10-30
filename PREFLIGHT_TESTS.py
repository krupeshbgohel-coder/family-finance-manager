#!/usr/bin/env python3
"""Offline package preflight checks; this is not an Android build or runtime test."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
errors, checks = [], []
def check(name, ok, detail=""):
    checks.append((name, bool(ok), detail))
    if not ok:
        errors.append(name)

required = [
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/familyfinance/manager/MainActivity.kt",
    "app/src/main/java/com/familyfinance/manager/data/FinanceDatabase.kt",
    "app/src/main/java/com/familyfinance/manager/cloud/FirebaseBootstrap.kt",
    "app/src/main/java/com/familyfinance/manager/cloud/FamilyCloudRepository.kt",
    "app/src/main/java/com/familyfinance/manager/cloud/CloudAuthScreen.kt",
    "app/src/main/res/values/firebase.xml",
    "app/src/main/res/values/strings.xml",
    "app/src/main/res/values-gu/strings.xml",
    "app/src/main/res/values-hi/strings.xml",
    "firestore.rules",
    ".github/workflows/build.yml",
]
for rel in required:
    check(f"required file: {rel}", (ROOT / rel).is_file())

xml_files = list(ROOT.rglob("*.xml"))
xml_ok = True
for p in xml_files:
    try:
        ET.parse(p)
    except Exception as exc:
        xml_ok = False
        errors.append(f"XML parse: {p.relative_to(ROOT)}: {exc}")
check("XML parsing", xml_ok, f"{len(xml_files)} XML files")

rules = (ROOT / "firestore.rules").read_text(encoding="utf-8")
check("Firestore default-deny rule", "allow read, write: if false" in rules)
check("Family membership rules present", "activeMember(familyId)" in rules and "match /members/{uid}" in rules)

repo = (ROOT / "app/src/main/java/com/familyfinance/manager/cloud/FamilyCloudRepository.kt").read_text(encoding="utf-8")
check("Firestore record helper present", all(x in repo for x in ["writeRecord", "readRecords", "deleteRecord"]))
check("Cloud sync fully wired", bool(re.search(r"Room.*Firestore|Firestore.*Room", repo, re.I)), "Expected to fail until sync adapter is implemented")

strings = {}
for rel in ["app/src/main/res/values/strings.xml", "app/src/main/res/values-gu/strings.xml", "app/src/main/res/values-hi/strings.xml"]:
    root = ET.parse(ROOT / rel).getroot()
    strings[rel] = {el.attrib.get("name") for el in root.findall("string")}
check("Starter language keys aligned", strings["app/src/main/res/values/strings.xml"] == strings["app/src/main/res/values-gu/strings.xml"] == strings["app/src/main/res/values-hi/strings.xml"], "Only starter strings are checked; this does not prove complete localization")

firebase = ET.parse(ROOT / "app/src/main/res/values/firebase.xml").getroot()
placeholders = [el.text for el in firebase.findall("string") if el.text and el.text.startswith("PASTE_")]
check("Firebase config clearly placeholder", len(placeholders) >= 4, f"{len(placeholders)} placeholder values")

for name, ok, detail in checks:
    print(("PASS" if ok else "FAIL") + " | " + name + (f" | {detail}" if detail else ""))
print(f"\nSummary: {sum(ok for _, ok, _ in checks)}/{len(checks)} checks passed")
print("Note: a failing integration check can be an expected release blocker, not a syntax test failure.")
sys.exit(1 if errors else 0)
