from abc import ABC, abstractmethod
from pathlib import Path
from typing import Generic, List, TypeVar, Union
import pandas as pd

T = TypeVar("T")


class BaseCsvParser(ABC, Generic[T]):
    """Abstract generic CSV parser enforcing strong typing and data structures."""

    def __init__(self, filepath: Union[str, Path]) -> None:
        self.filepath: Path = Path(filepath)
        self.df: pd.DataFrame = pd.DataFrame()
        self.records: List[T] = []

    def load(self) -> List[T]:
        """Loads CSV, standardizes columns, and populates typed data structures."""
        if not self.filepath.exists():
            raise FileNotFoundError(f"File not found: {self.filepath}")

        self.df = pd.read_csv(self.filepath)
        self.df.columns = self.df.columns.str.lower()
        self.records = self._parse_records()
        return self.records

    @abstractmethod
    def _parse_records(self) -> List[T]:
        """Converts internal DataFrame rows into strongly-typed dataclass instances."""
        pass

    def get_records(self) -> List[T]:
        """Returns the typed dataset records."""
        if not self.records and not self.df.empty:
            self.records = self._parse_records()
        elif not self.records:
            return self.load()
        return self.records