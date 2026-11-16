from typing import Annotated

from pydantic import AfterValidator, BaseModel, ConfigDict
from pydantic.alias_generators import to_camel


class CamelModel(BaseModel):
    """Request/response model whose JSON uses camelCase, like the Java and .NET APIs."""

    model_config = ConfigDict(alias_generator=to_camel, populate_by_name=True)


def _not_blank(value: str) -> str:
    if not value.strip():
        raise ValueError("must not be blank")
    return value


# Equivalent of Java's @NotBlank / .NET's [Required(AllowEmptyStrings = false)].
NotBlankStr = Annotated[str, AfterValidator(_not_blank)]
