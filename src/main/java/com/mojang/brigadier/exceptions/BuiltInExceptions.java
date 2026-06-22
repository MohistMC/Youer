package com.mojang.brigadier.exceptions;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;

public class BuiltInExceptions implements BuiltInExceptionProvider
{
    private static final Dynamic2CommandExceptionType DOUBLE_TOO_SMALL;
    private static final Dynamic2CommandExceptionType DOUBLE_TOO_BIG;
    private static final Dynamic2CommandExceptionType FLOAT_TOO_SMALL;
    private static final Dynamic2CommandExceptionType FLOAT_TOO_BIG;
    private static final Dynamic2CommandExceptionType INTEGER_TOO_SMALL;
    private static final Dynamic2CommandExceptionType INTEGER_TOO_BIG;
    private static final Dynamic2CommandExceptionType LONG_TOO_SMALL;
    private static final Dynamic2CommandExceptionType LONG_TOO_BIG;
    private static final DynamicCommandExceptionType LITERAL_INCORRECT;
    private static final SimpleCommandExceptionType READER_EXPECTED_START_OF_QUOTE;
    private static final SimpleCommandExceptionType READER_EXPECTED_END_OF_QUOTE;
    private static final DynamicCommandExceptionType READER_INVALID_ESCAPE;
    private static final DynamicCommandExceptionType READER_INVALID_BOOL;
    private static final DynamicCommandExceptionType READER_INVALID_INT;
    private static final SimpleCommandExceptionType READER_EXPECTED_INT;
    private static final DynamicCommandExceptionType READER_INVALID_LONG;
    private static final SimpleCommandExceptionType READER_EXPECTED_LONG;
    private static final DynamicCommandExceptionType READER_INVALID_DOUBLE;
    private static final SimpleCommandExceptionType READER_EXPECTED_DOUBLE;
    private static final DynamicCommandExceptionType READER_INVALID_FLOAT;
    private static final SimpleCommandExceptionType READER_EXPECTED_FLOAT;
    private static final SimpleCommandExceptionType READER_EXPECTED_BOOL;
    private static final DynamicCommandExceptionType READER_EXPECTED_SYMBOL;
    private static final SimpleCommandExceptionType DISPATCHER_UNKNOWN_COMMAND;
    private static final SimpleCommandExceptionType DISPATCHER_UNKNOWN_ARGUMENT;
    private static final SimpleCommandExceptionType DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR;
    private static final DynamicCommandExceptionType DISPATCHER_PARSE_EXCEPTION;
    
    @Override
    public Dynamic2CommandExceptionType doubleTooLow() {
        return BuiltInExceptions.DOUBLE_TOO_SMALL;
    }
    
    @Override
    public Dynamic2CommandExceptionType doubleTooHigh() {
        return BuiltInExceptions.DOUBLE_TOO_BIG;
    }
    
    @Override
    public Dynamic2CommandExceptionType floatTooLow() {
        return BuiltInExceptions.FLOAT_TOO_SMALL;
    }
    
    @Override
    public Dynamic2CommandExceptionType floatTooHigh() {
        return BuiltInExceptions.FLOAT_TOO_BIG;
    }
    
    @Override
    public Dynamic2CommandExceptionType integerTooLow() {
        return BuiltInExceptions.INTEGER_TOO_SMALL;
    }
    
    @Override
    public Dynamic2CommandExceptionType integerTooHigh() {
        return BuiltInExceptions.INTEGER_TOO_BIG;
    }
    
    @Override
    public Dynamic2CommandExceptionType longTooLow() {
        return BuiltInExceptions.LONG_TOO_SMALL;
    }
    
    @Override
    public Dynamic2CommandExceptionType longTooHigh() {
        return BuiltInExceptions.LONG_TOO_BIG;
    }
    
    @Override
    public DynamicCommandExceptionType literalIncorrect() {
        return BuiltInExceptions.LITERAL_INCORRECT;
    }
    
    @Override
    public SimpleCommandExceptionType readerExpectedStartOfQuote() {
        return BuiltInExceptions.READER_EXPECTED_START_OF_QUOTE;
    }
    
    @Override
    public SimpleCommandExceptionType readerExpectedEndOfQuote() {
        return BuiltInExceptions.READER_EXPECTED_END_OF_QUOTE;
    }
    
    @Override
    public DynamicCommandExceptionType readerInvalidEscape() {
        return BuiltInExceptions.READER_INVALID_ESCAPE;
    }
    
    @Override
    public DynamicCommandExceptionType readerInvalidBool() {
        return BuiltInExceptions.READER_INVALID_BOOL;
    }
    
    @Override
    public DynamicCommandExceptionType readerInvalidInt() {
        return BuiltInExceptions.READER_INVALID_INT;
    }
    
    @Override
    public SimpleCommandExceptionType readerExpectedInt() {
        return BuiltInExceptions.READER_EXPECTED_INT;
    }
    
    @Override
    public DynamicCommandExceptionType readerInvalidLong() {
        return BuiltInExceptions.READER_INVALID_LONG;
    }
    
    @Override
    public SimpleCommandExceptionType readerExpectedLong() {
        return BuiltInExceptions.READER_EXPECTED_LONG;
    }
    
    @Override
    public DynamicCommandExceptionType readerInvalidDouble() {
        return BuiltInExceptions.READER_INVALID_DOUBLE;
    }
    
    @Override
    public SimpleCommandExceptionType readerExpectedDouble() {
        return BuiltInExceptions.READER_EXPECTED_DOUBLE;
    }
    
    @Override
    public DynamicCommandExceptionType readerInvalidFloat() {
        return BuiltInExceptions.READER_INVALID_FLOAT;
    }
    
    @Override
    public SimpleCommandExceptionType readerExpectedFloat() {
        return BuiltInExceptions.READER_EXPECTED_FLOAT;
    }
    
    @Override
    public SimpleCommandExceptionType readerExpectedBool() {
        return BuiltInExceptions.READER_EXPECTED_BOOL;
    }
    
    @Override
    public DynamicCommandExceptionType readerExpectedSymbol() {
        return BuiltInExceptions.READER_EXPECTED_SYMBOL;
    }
    
    @Override
    public SimpleCommandExceptionType dispatcherUnknownCommand() {
        return BuiltInExceptions.DISPATCHER_UNKNOWN_COMMAND;
    }
    
    @Override
    public SimpleCommandExceptionType dispatcherUnknownArgument() {
        return BuiltInExceptions.DISPATCHER_UNKNOWN_ARGUMENT;
    }
    
    @Override
    public SimpleCommandExceptionType dispatcherExpectedArgumentSeparator() {
        return BuiltInExceptions.DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR;
    }
    
    @Override
    public DynamicCommandExceptionType dispatcherParseException() {
        return BuiltInExceptions.DISPATCHER_PARSE_EXCEPTION;
    }
    
    static {
        DOUBLE_TOO_SMALL = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage("Double must not be less than " + min + ", found " + found));
        DOUBLE_TOO_BIG = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage("Double must not be more than " + max + ", found " + found));
        FLOAT_TOO_SMALL = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage("Float must not be less than " + min + ", found " + found));
        FLOAT_TOO_BIG = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage("Float must not be more than " + max + ", found " + found));
        INTEGER_TOO_SMALL = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage("Integer must not be less than " + min + ", found " + found));
        INTEGER_TOO_BIG = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage("Integer must not be more than " + max + ", found " + found));
        LONG_TOO_SMALL = new Dynamic2CommandExceptionType((found, min) -> new LiteralMessage("Long must not be less than " + min + ", found " + found));
        LONG_TOO_BIG = new Dynamic2CommandExceptionType((found, max) -> new LiteralMessage("Long must not be more than " + max + ", found " + found));
        LITERAL_INCORRECT = new DynamicCommandExceptionType(expected -> new LiteralMessage("Expected literal " + expected));
        READER_EXPECTED_START_OF_QUOTE = new SimpleCommandExceptionType(new LiteralMessage("Expected quote to start a string"));
        READER_EXPECTED_END_OF_QUOTE = new SimpleCommandExceptionType(new LiteralMessage("Unclosed quoted string"));
        READER_INVALID_ESCAPE = new DynamicCommandExceptionType(character -> new LiteralMessage("Invalid escape sequence '" + character + "' in quoted string"));
        READER_INVALID_BOOL = new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid bool, expected true or false but found '" + value + "'"));
        READER_INVALID_INT = new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid integer '" + value + "'"));
        READER_EXPECTED_INT = new SimpleCommandExceptionType(new LiteralMessage("Expected integer"));
        READER_INVALID_LONG = new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid long '" + value + "'"));
        READER_EXPECTED_LONG = new SimpleCommandExceptionType(new LiteralMessage("Expected long"));
        READER_INVALID_DOUBLE = new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid double '" + value + "'"));
        READER_EXPECTED_DOUBLE = new SimpleCommandExceptionType(new LiteralMessage("Expected double"));
        READER_INVALID_FLOAT = new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid float '" + value + "'"));
        READER_EXPECTED_FLOAT = new SimpleCommandExceptionType(new LiteralMessage("Expected float"));
        READER_EXPECTED_BOOL = new SimpleCommandExceptionType(new LiteralMessage("Expected bool"));
        READER_EXPECTED_SYMBOL = new DynamicCommandExceptionType(symbol -> new LiteralMessage("Expected '" + symbol + "'"));
        DISPATCHER_UNKNOWN_COMMAND = new SimpleCommandExceptionType(new LiteralMessage("Unknown command"));
        DISPATCHER_UNKNOWN_ARGUMENT = new SimpleCommandExceptionType(new LiteralMessage("Incorrect argument for command"));
        DISPATCHER_EXPECTED_ARGUMENT_SEPARATOR = new SimpleCommandExceptionType(new LiteralMessage("Expected whitespace to end one argument, but found trailing data"));
        DISPATCHER_PARSE_EXCEPTION = new DynamicCommandExceptionType(message -> new LiteralMessage("Could not parse command: " + message));
    }
}
