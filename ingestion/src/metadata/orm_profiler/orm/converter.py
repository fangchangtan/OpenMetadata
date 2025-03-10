#  Copyright 2021 Collate
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#  http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

"""
Converter logic to transform an OpenMetadata Table Entity
to an SQLAlchemy ORM class.
"""
from functools import singledispatch
from typing import Union

import sqlalchemy
from sqlalchemy.orm import DeclarativeMeta, declarative_base

from metadata.generated.schema.entity.data.database import Database
from metadata.generated.schema.entity.data.table import Column, DataType, Table
from metadata.orm_profiler.orm.registry import CustomTypes

Base = declarative_base()

_TYPE_MAP = {
    DataType.NUMBER: sqlalchemy.INTEGER,
    DataType.TINYINT: sqlalchemy.SMALLINT,
    DataType.SMALLINT: sqlalchemy.SMALLINT,
    DataType.INT: sqlalchemy.INT,
    DataType.BIGINT: sqlalchemy.BIGINT,
    DataType.BYTEINT: sqlalchemy.SMALLINT,
    DataType.BYTES: CustomTypes.BYTES,
    DataType.FLOAT: sqlalchemy.FLOAT,
    DataType.DOUBLE: sqlalchemy.DECIMAL,
    DataType.DECIMAL: sqlalchemy.DECIMAL,
    DataType.NUMERIC: sqlalchemy.NUMERIC,
    DataType.TIMESTAMP: sqlalchemy.TIMESTAMP,
    DataType.TIME: sqlalchemy.TIME,
    DataType.DATE: sqlalchemy.DATE,
    DataType.DATETIME: sqlalchemy.DATETIME,
    DataType.INTERVAL: sqlalchemy.Interval,
    DataType.STRING: sqlalchemy.String,
    DataType.MEDIUMTEXT: sqlalchemy.TEXT,
    DataType.TEXT: sqlalchemy.TEXT,
    DataType.CHAR: sqlalchemy.CHAR,
    DataType.VARCHAR: sqlalchemy.VARCHAR,
    DataType.BOOLEAN: sqlalchemy.BOOLEAN,
    DataType.BINARY: sqlalchemy.BINARY,
    DataType.VARBINARY: sqlalchemy.VARBINARY,
    # DataType.ARRAY: sqlalchemy.ARRAY,
    DataType.BLOB: sqlalchemy.BLOB,
    DataType.LONGBLOB: sqlalchemy.LargeBinary,
    DataType.MEDIUMBLOB: sqlalchemy.LargeBinary,
    # DataType.MAP: ...,
    # DataType.STRUCT: ...,
    # DataType.UNION: ...,
    # DataType.SET: ...,
    # DataType.GEOGRAPHY: ...,
    DataType.ENUM: sqlalchemy.Enum,
    DataType.JSON: sqlalchemy.JSON,
    DataType.UUID: CustomTypes.UUID,
}


def build_orm_col(idx: int, col: Column) -> sqlalchemy.Column:
    """
    Cook the ORM column from our metadata instance
    information.

    The first parsed column will be used arbitrarily
    as the PK, as SQLAlchemy forces us to specify
    at least one PK.

    As this is only used for INSERT/UPDATE/DELETE,
    there is no impact for our read-only purposes.
    """
    return sqlalchemy.Column(
        name=str(col.name.__root__),
        type_=_TYPE_MAP.get(col.dataType),
        primary_key=True if idx == 0 else False,
    )


def ometa_to_orm(table: Table, database: Union[Database, str]) -> DeclarativeMeta:
    """
    Given an OpenMetadata instance, prepare
    the SQLAlchemy DeclarativeMeta class
    to run queries on top of it.

    We are building the class dynamically using
    `type` and passing SQLAlchemy `Base` class
    as the bases tuple for inheritance.
    """

    cols = {
        str(col.name.__root__): build_orm_col(idx, col)
        for idx, col in enumerate(table.columns)
    }

    # Type takes positional arguments in the form of (name, bases, dict)
    orm = type(
        table.fullyQualifiedName.replace(".", "_"),  # Output class name
        (Base,),  # SQLAlchemy declarative base
        {
            "__tablename__": str(table.name.__root__),
            "__table_args__": {
                "schema": get_db_name(database),
                "extend_existing": True,  # Recreates the table ORM object if it already exists. Useful for testing
            },
            **cols,
        },
    )

    if not isinstance(orm, DeclarativeMeta):
        raise ValueError("OMeta to ORM did not create a DeclarativeMeta")

    return orm


@singledispatch
def get_db_name(arg) -> str:
    """
    Return the database name to pass the table schema info
    to the ORM object.

    :param arg: Database or str
    :return: db name
    """
    raise NotImplementedError(f"Cannot extract db name from {arg}")


@get_db_name.register
def _(arg: str) -> str:
    """
    Return string as is

    :param arg: string
    :return: db name
    """
    return arg


@get_db_name.register
def _(arg: Database) -> str:
    """
    Get the db name from the database entity

    :param arg: database
    :return: db name
    """
    return str(arg.name.__root__)
