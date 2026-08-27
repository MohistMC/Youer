package com.mohistmc.youer.ai.model;

public sealed interface AiContentPart permits AiTextContent, AiToolCallContent, AiToolResultContent {
}
