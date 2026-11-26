#!/usr/bin/env python3
"""Validate data/schedule.json before it is published to GitHub."""

from __future__ import annotations

import json
import re
import sys
from datetime import date, datetime
from pathlib import Path

TIME_RE = re.compile(r"^(\d{1,2}):(\d{2})-(\d{1,2}):(\d{2})$")
DAYS = ["monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"]
KINDS = {"replace", "replacement", "замена", "extra", "add", "доп", "дополнительная", "cancel", "cancelled", "отмена", "note", "info", "примечание", "пометка"}
ERRORS: list[str] = []


def fail(message: str) -> None:
    ERRORS.append(message)


def parse_date(value: object, field: str, required: bool = True) -> date | None:
    if value is None:
        if required:
            fail(f"{field}: отсутствует дата")
        return None
    if not isinstance(value, str):
        fail(f"{field}: дата должна быть строкой")
        return None
    try:
        return date.fromisoformat(value)
    except ValueError:
        fail(f"{field}: неверная дата {value!r}, нужен формат YYYY-MM-DD")
        return None


def validate_time(value: object, field: str) -> None:
    if not isinstance(value, str) or not TIME_RE.match(value):
        fail(f"{field}: время должно быть в формате 14:10-15:50")
        return
    match = TIME_RE.match(value)
    assert match is not None
    sh, sm, eh, em = map(int, match.groups())
    if sh > 23 or eh > 23 or sm > 59 or em > 59:
        fail(f"{field}: некорректные часы/минуты в {value!r}")
    if (eh * 60 + em) <= (sh * 60 + sm):
        fail(f"{field}: конец пары должен быть позже начала ({value!r})")


def validate_weeks(value: object, field: str) -> None:
    if value is None:
        return
    if value == "all":
        return
    if not isinstance(value, list) or not value:
        fail(f"{field}: weeks должно быть \"all\" или непустым массивом номеров недель")
        return
    if any(not isinstance(item, int) or item < 1 for item in value):
        fail(f"{field}: номера недель должны быть положительными целыми")


def validate_lesson(item: object, field: str) -> None:
    if not isinstance(item, dict):
        fail(f"{field}: элемент должен быть объектом")
        return
    validate_time(item.get("time"), f"{field}.time")
    subject = item.get("subject")
    if not isinstance(subject, str) or not subject.strip():
        fail(f"{field}.subject: обязательное непустое поле")
    room = item.get("room")
    if room is not None and not isinstance(room, str):
        fail(f"{field}.room: должно быть строкой")
    teachers = item.get("teachers")
    if teachers is not None:
        if not isinstance(teachers, list) or any(not isinstance(t, str) for t in teachers):
            fail(f"{field}.teachers: должен быть массивом строк")
    validate_weeks(item.get("weeks"), f"{field}.weeks")


def validate_change(item: object, field: str) -> None:
    if not isinstance(item, dict):
        fail(f"{field}: элемент должен быть объектом")
        return
    parse_date(item.get("date"), f"{field}.date")
    kind = item.get("kind")
    if not isinstance(kind, str) or kind.lower() not in KINDS:
        fail(f"{field}.kind: ожидается replace, extra, cancel или note")
    validate_time(item.get("time"), f"{field}.time")
    for key in ("subject", "room", "note"):
        value = item.get(key)
        if value is not None and not isinstance(value, str):
            fail(f"{field}.{key}: должно быть строкой")
    teachers = item.get("teachers")
    if teachers is not None:
        if not isinstance(teachers, list) or any(not isinstance(t, str) for t in teachers):
            fail(f"{field}.teachers: должен быть массивом строк")


def validate_work(item: object, field: str) -> None:
    if not isinstance(item, dict):
        fail(f"{field}: элемент должен быть объектом")
        return
    parse_date(item.get("date"), f"{field}.date")
    subject = item.get("subject")
    if not isinstance(subject, str) or not subject.strip():
        fail(f"{field}.subject: обязательное непустое поле")
    for key in ("type", "title", "note"):
        value = item.get(key)
        if value is not None and not isinstance(value, str):
            fail(f"{field}.{key}: должно быть строкой")


def main() -> int:
    path = Path(sys.argv[1] if len(sys.argv) > 1 else "data/schedule.json")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        print(f"ERROR: file not found: {path}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as exc:
        print(f"ERROR: invalid JSON in {path}: {exc}", file=sys.stderr)
        return 1

    if not isinstance(data, dict):
        fail("корень JSON должен быть объектом")
    else:
        for key in ("group", "semester", "academic_year", "semester_start", "schedule"):
            if key not in data:
                fail(f"отсутствует обязательное поле {key!r}")
        if isinstance(data.get("group"), str) and not data["group"].strip():
            fail("group не может быть пустым")
        parse_date(data.get("semester_start"), "semester_start")
        updated = data.get("updated_at")
        if updated is not None:
            try:
                datetime.fromisoformat(str(updated).replace("Z", "+00:00"))
            except ValueError:
                fail("updated_at: нужен формат ISO-8601, например 2026-11-26T18:00:00+03:00")

        schedule = data.get("schedule")
        if not isinstance(schedule, dict):
            fail("schedule должен быть объектом")
        else:
            for day in DAYS:
                lessons = schedule.get(day)
                if lessons is None:
                    fail(f"schedule.{day}: отсутствует (для пустого дня укажите [])")
                elif not isinstance(lessons, list):
                    fail(f"schedule.{day}: должен быть массивом")
                else:
                    for index, lesson in enumerate(lessons):
                        validate_lesson(lesson, f"schedule.{day}[{index}]")

        changes = data.get("changes", [])
        if changes is not None:
            if not isinstance(changes, list):
                fail("changes должен быть массивом")
            else:
                for index, change in enumerate(changes):
                    validate_change(change, f"changes[{index}]")

        works = data.get("works", [])
        if works is not None:
            if not isinstance(works, list):
                fail("works должен быть массивом")
            else:
                for index, work in enumerate(works):
                    validate_work(work, f"works[{index}]")

    if ERRORS:
        print(f"Validation failed for {path}:", file=sys.stderr)
        for error in ERRORS:
            print(f"  - {error}", file=sys.stderr)
        return 1

    lessons = sum(len(v) for v in data["schedule"].values())
    print(
        f"OK: {data.get('group', '?')} · {data.get('semester', '?')} семестр · "
        f"{lessons} пар, {len(data.get('changes', []))} изменений, "
        f"{len(data.get('works', []))} работ"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
