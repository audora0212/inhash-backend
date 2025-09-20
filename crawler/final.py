import json
import time
import os
from typing import Any, Dict, List

from base_scraper import fetch_all, build_due_summary


def flatten_due_items(due_summary: Dict[str, Any]) -> List[Dict[str, Any]]:
	items: List[Dict[str, Any]] = []
	for section in ("upcoming", "overdue"):
		for kind in ("assignments", "classes"):
			for it in due_summary.get(section, {}).get(kind, []):
				entry = {
					"status": section,
					"type": it.get("type"),
					"course_name": it.get("course_name"),
					"title": it.get("title"),
					"due": it.get("due"),
					"url": it.get("url"),
					"remaining_seconds": it.get("remaining_seconds"),
				}
				items.append(entry)
	return items


def humanize_remaining(seconds: int) -> str:
	try:
		abs_s = abs(int(seconds))
		days = abs_s // 86400
		hours = (abs_s % 86400) // 3600
		mins = (abs_s % 3600) // 60
		prefix = "-" if seconds < 0 else ""
		if days > 0:
			return f"{prefix}{days}일 {hours}시간 {mins}분"
		if hours > 0:
			return f"{prefix}{hours}시간 {mins}분"
		return f"{prefix}{mins}분"
	except Exception:
		return "N/A"


def main() -> None:
	# 1) 데이터 수집
	username = os.getenv("INHASH_USERNAME", "")
	password = os.getenv("INHASH_PASSWORD", "")
	if not username or not password:
		raise SystemExit("INHASH_USERNAME/INHASH_PASSWORD env required")
	full = fetch_all(username, password)
	# 2) 마감 요약 구성
	due = build_due_summary(full)

	# 2.5) 지난 항목 제외: upcoming만 유지
	due_filtered = {
		"upcoming": {
			"assignments": due.get("upcoming", {}).get("assignments", []),
			"classes": due.get("upcoming", {}).get("classes", []),
		},
		"overdue": {
			"assignments": [],
			"classes": [],
		}
	}

	# 3) 남은 시간 기준 통합 정렬 목록 생성 (가까운 마감부터)
	all_items = flatten_due_items(due_filtered)
	all_items.sort(key=lambda x: (x["status"] != "upcoming", x.get("remaining_seconds", 9e18)))

	# 4) 출력 요약 프린트
	print("\n[마감 임박 일정] (upcoming 먼저, overdue는 뒤에)\n")
	for it in all_items:
		rem = it.get("remaining_seconds")
		rem_text = humanize_remaining(rem) if isinstance(rem, int) else "N/A"
		print(f"[{it['status']}] {it['course_name']} - {it['title']} | due: {it['due']} | 남은시간: {rem_text}")
		if it.get("url"):
			print(f"  ↳ {it['url']}")

	# 5) 파일 저장 (경로 우선순위: INHASH_OUTPUT_PATH > output/final.json)
	courses = [
		{
			"id": c.get("id"),
			"name": c.get("name"),
			"main_link": c.get("main_link"),
		}
		for c in full.get("courses", [])
	]
	output_path = os.getenv("INHASH_OUTPUT_PATH", "output/final.json")
	# Ensure directory exists
	dirname = os.path.dirname(output_path)
	if dirname:
		os.makedirs(dirname, exist_ok=True)
	with open(output_path, "w", encoding="utf-8") as f:
		json.dump({
			"generated_at": time.strftime("%Y-%m-%d %H:%M:%S"),
			"items": all_items,
			"courses": courses,
		}, f, ensure_ascii=False, indent=2)
	print(f"\n저장 완료: {output_path}")


if __name__ == "__main__":
	main()


