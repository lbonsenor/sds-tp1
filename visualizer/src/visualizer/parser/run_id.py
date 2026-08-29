import re
from dataclasses import dataclass
from typing import Optional

# Engine builds run_id as: <prefix>_<timestamp>_run<iteration>
# e.g. "std_d4_eta1.5_1735492800_run3"
_RUN_ID_PATTERN = re.compile(r"^(?P<prefix>.+)_(?P<timestamp>\d+)_run(?P<iteration>\d+)$")


@dataclass(frozen=True)
class RunIdComponents:
    prefix: str
    timestamp: str
    iteration: int
    raw: str


def parse_run_id(run_id: str) -> Optional[RunIdComponents]:
    match = _RUN_ID_PATTERN.match(run_id)
    if not match:
        return None
    return RunIdComponents(
        prefix=match.group("prefix"),
        timestamp=match.group("timestamp"),
        iteration=int(match.group("iteration")),
        raw=run_id,
    )


def run_prefix(run_id: str) -> str:
    parsed = parse_run_id(run_id)
    return parsed.prefix if parsed is not None else run_id