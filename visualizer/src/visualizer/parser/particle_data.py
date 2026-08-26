import re
from dataclasses import dataclass
from pathlib import Path
from typing import List, Union
import pandas as pd
from .base_parser import BaseCsvParser


@dataclass(frozen=True)
class ParticleDataRecord:
    run_id: str
    time: float
    particle_id: int
    x: float
    y: float
    radius: float
    neighbors_raw: str
    neighbors_list: List[int]
    neighbor_count: int


class ParticleDataParser(BaseCsvParser[ParticleDataRecord]):

    def _parse_records(self) -> List[ParticleDataRecord]:
        records: List[ParticleDataRecord] = []
        for _, row in self.df.iterrows():
            raw_neighbors = str(row["neighbors"])
            n_list = self._parse_neighbors(raw_neighbors)
            record = ParticleDataRecord(
                run_id=str(row["run_id"]),
                time=float(row["time"]),
                particle_id=int(row["particle_id"]),
                x=float(row["x"]),
                y=float(row["y"]),
                radius=float(row["radius"]),
                neighbors_raw=raw_neighbors,
                neighbors_list=n_list,
                neighbor_count=len(n_list),
            )
            records.append(record)
        return records

    @staticmethod
    def _parse_neighbors(val: Union[str, float]) -> List[int]:
        """Parses bracket strings like '[0 2]' or 'none' to List[int]."""
        if pd.isna(val) or val == "none" or not str(val).strip():
            return []
        s = str(val).strip().lstrip("[").rstrip("]").strip()
        if not s:
            return []
        return [int(p) for p in re.split(r"[\s,]+", s) if p.isdigit()]