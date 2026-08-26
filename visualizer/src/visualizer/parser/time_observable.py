from dataclasses import dataclass
from pathlib import Path
from typing import List, Union
from .base_parser import BaseCsvParser


@dataclass(frozen=True)
class TimeObservableRecord:
    run_id: str
    model: str
    density: float
    eta: float
    time: float
    va: float
    cluster_ratio: float
    max_cluster_size: int


class TimeObservableParser(BaseCsvParser[TimeObservableRecord]):

    def _parse_records(self) -> List[TimeObservableRecord]:
        records: List[TimeObservableRecord] = []
        for _, row in self.df.iterrows():
            record = TimeObservableRecord(
                run_id=str(row["run_id"]),
                model=str(row["model"]),
                density=float(row["density"]),
                eta=float(row["eta"]),
                time=float(row["time"]),
                va=float(row["va"]),
                cluster_ratio=float(row["cluster_ratio"]),
                max_cluster_size=int(row["max_cluster_size"]),
            )
            records.append(record)
        return records

    def get_polarization_at_time(self, time: float) -> List[TimeObservableRecord]:
        """Filters observable records at a specific time step."""
        return [r for r in self.records if r.time == time]