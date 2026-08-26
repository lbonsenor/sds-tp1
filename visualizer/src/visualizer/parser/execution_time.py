from dataclasses import dataclass
from pathlib import Path
from typing import List, Union
from .base_parser import BaseCsvParser


@dataclass(frozen=True)
class ExecutionTimeRecord:
    run_id: str
    model: str
    density: float
    n_particles: int
    method: str
    execution_time_sec: float
    execution_time_ms: float
    n_steps: int
    box_length: float
    interaction_radius: float


class ExecutionTimeParser(BaseCsvParser[ExecutionTimeRecord]):

    def _parse_records(self) -> List[ExecutionTimeRecord]:
        records: List[ExecutionTimeRecord] = []
        for _, row in self.df.iterrows():
            sec = float(row["execution_time_sec"])
            record = ExecutionTimeRecord(
                run_id=str(row["run_id"]),
                model=str(row["model"]),
                density=float(row["density"]),
                n_particles=int(row["n_particles"]),
                method=str(row["method"]),
                execution_time_sec=sec,
                execution_time_ms=sec * 1000.0,
                n_steps=int(row["n_steps"]),
                box_length=float(row["box_length"]),
                interaction_radius=float(row["interaction_radius"]),
            )
            records.append(record)
        return records

    def get_average_execution_time(self) -> float:
        """Calculates average step execution time in seconds."""
        if not self.records:
            return 0.0
        return sum(r.execution_time_sec for r in self.records) / len(self.records)