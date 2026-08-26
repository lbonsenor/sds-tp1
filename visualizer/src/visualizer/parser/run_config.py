from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Union
from .base_parser import BaseCsvParser


@dataclass(frozen=True)
class RunConfigRecord:
    run_id: str
    model: str
    length: int
    cut_off: float
    min_radius: float
    max_radius: float
    cell_grid_split: int
    n_particles: int
    periodic_boundary: bool
    delta_time: float
    total_time: float
    eta: float
    seed: int


class RunConfigParser(BaseCsvParser[RunConfigRecord]):

    def _parse_records(self) -> List[RunConfigRecord]:
        records: List[RunConfigRecord] = []
        for _, row in self.df.iterrows():
            record = RunConfigRecord(
                run_id=str(row["run_id"]),
                model=str(row["model"]),
                length=int(row["length"]),
                cut_off=float(row["cut_off"]),
                min_radius=float(row["min_radius"]),
                max_radius=float(row["max_radius"]),
                cell_grid_split=int(row["cell_grid_split"]),
                n_particles=int(row["n_particles"]),
                periodic_boundary=bool(row["periodic_boundary"]),
                delta_time=float(row["delta_time"]),
                total_time=float(row["total_time"]),
                eta=float(row["eta"]),
                seed=int(row["seed"]),
            )
            records.append(record)
        return records

    def get_config_by_run_id(self, run_id: str) -> Optional[RunConfigRecord]:
        """Retrieves configuration for a specific run ID."""
        for rec in self.records:
            if rec.run_id == run_id:
                return rec
        return None