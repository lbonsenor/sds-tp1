from dataclasses import dataclass
from pathlib import Path
from typing import List, Union
from .base_parser import BaseCsvParser


@dataclass(frozen=True)
class ClusterDetailRecord:
    run_id: str
    time: float
    cluster_id: int
    size: int


class ClusterDetailParser(BaseCsvParser[ClusterDetailRecord]):

    def _parse_records(self) -> List[ClusterDetailRecord]:
        records: List[ClusterDetailRecord] = []
        for _, row in self.df.iterrows():
            record = ClusterDetailRecord(
                run_id=str(row["run_id"]),
                time=float(row["time"]),
                cluster_id=int(row["cluster_id"]),
                size=int(row["size"]),
            )
            records.append(record)
        return records

    def get_clusters_at_time(self, time: float) -> List[ClusterDetailRecord]:
        """Returns cluster breakdown for a specific timestep."""
        return [r for r in self.records if r.time == time]