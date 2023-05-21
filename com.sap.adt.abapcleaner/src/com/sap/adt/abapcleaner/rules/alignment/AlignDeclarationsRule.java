package com.sap.adt.abapcleaner.rules.alignment;

import java.time.LocalDate;
import java.util.*;

import com.sap.adt.abapcleaner.base.*;
import com.sap.adt.abapcleaner.parser.*;
import com.sap.adt.abapcleaner.programbase.*;
import com.sap.adt.abapcleaner.rulebase.*;
import com.sap.adt.abapcleaner.rulehelpers.*;

public class AlignDeclarationsRule extends AlignDeclarationSectionRuleBase {
   // for the structure of DATA statements, see https://help.sap.com/doc/abapdocu_latest_index_htm/latest/en-US/index.htm?file=abapdata.htm

   private enum Columns  {
      KEYWORD,
      IDENTIFIER,
      TYPE,
      LENGTH,
      DECIMALS,
      VALUE,
      READ_ONLY,
      LINE_END_COMMENT;

		public int getValue() { return this.ordinal(); }
	}

	private static final int MAX_COLUMN_COUNT = 8;

	private final static RuleReference[] references = new RuleReference[] { 
			new RuleReference(RuleSource.ABAP_CLEANER),
			new RuleReference(RuleSource.ABAP_STYLE_GUIDE, "Don't align type clauses", "#dont-align-type-clauses", true) };

   private class TableStart {
   	public final int firstLineBreaks;
   	public final int keywordIndent;
   	public final int basicIdentifierIndent;
   	
   	private TableStart(int firstLineBreaks, int keywordIndent, int basicIdentifierIndent) {
   		this.firstLineBreaks = firstLineBreaks;
   		this.keywordIndent = keywordIndent;
   		this.basicIdentifierIndent = basicIdentifierIndent;
   	}
   }

   /** Creates a set of {@link AlignTable}s, which are then aligned independently. 
    * Declarations outside of BEGIN OF ... END OF sections are aligned with the first table. 
    * For nested BEGIN OF ... END OF sections, a stack is used to fill one or several {@link AlignTable} instances, 
    * depending on the supplied {@link StructureAlignStyle}. */
   private class TableSet {
   	private final StructureAlignStyle structureAlignStyle;
   	public int additionalIndent;
   	
   	private Stack<AlignTable> tableStack = new Stack<>();
   	private ArrayList<AlignTable> allTables = new ArrayList<>();
   	
   	public TableSet(StructureAlignStyle structureAlignStyle) {
   		this.structureAlignStyle = structureAlignStyle;
   		addTable();
   	}
   	
   	private void addTable() {
   		AlignTable newTable = new AlignTable(MAX_COLUMN_COUNT);
   		allTables.add(newTable);
  			tableStack.push(newTable);
   	}
   	
   	public AlignTable getCurrentTable() {
   		// in special cases, e.g. if the rule is executed on a "TYPES END OF" line, the table stack may be empty
   		if (tableStack.isEmpty()) {
      		addTable();
   		}
   		return tableStack.peek(); 
   	}

   	public void beginOfStructure() {
   		if (structureAlignStyle == StructureAlignStyle.ACROSS_LEVELS && additionalIndent > 0) {
   			// continue using the table at the top of the table stack, which was started at the top-level BEGIN OF
   		} else {
   			addTable();
   		}
  			additionalIndent += 2;
   	}
   	
   	public void endOfStructure() {
   		if (additionalIndent > 0) {
   			additionalIndent -= 2;
   		}
   		if (!tableStack.isEmpty()) {
   			if (structureAlignStyle == StructureAlignStyle.ACROSS_LEVELS && additionalIndent > 0) {
      			// continue using the table at the top of the table stack, which was started at the top-level BEGIN OF
   			} else {
   				tableStack.pop();
   			}
   		}
   		
   		if (structureAlignStyle == StructureAlignStyle.PER_SECTION || additionalIndent == 0) {
   			// replace the table from the previous level with a new one, which will have independent alignment
   			if (!tableStack.isEmpty())
   				tableStack.pop();
   			addTable();
   		}
   	}

   	public Iterable<AlignTable> getAllTables() { 
   		return allTables; 
  		}
   }
   
	@Override
	public RuleID getID() { return RuleID.ALIGN_DECLARATIONS; }

	@Override
	public RuleGroupID getGroupID() { return RuleGroupID.ALIGNMENT; }

	@Override
	public String getDisplayName() { return "Align declarations"; }

	@Override
	public String getDescription() { return "Aligns both chains and consecutive declaration lines of CONSTANTS, DATA, FIELD-SYMBOLS, and TYPES."; }

	@Override
	public LocalDate getDateCreated() { return LocalDate.of(2021, 1, 8); }

	@Override
	public RuleReference[] getReferences() { return references; }

   @Override
   public String getExample() {
      return  LINE_SEP + "  METHOD align_declarations." 
				+ LINE_SEP + "    CONSTANTS: lc_any_price TYPE ty_price VALUE 1200," 
				+ LINE_SEP + "      lc_other_price TYPE ty_price VALUE 1000," 
				+ LINE_SEP + "               lc_num_contract_change TYPE ty_sequence_number VALUE    1," 
				+ LINE_SEP + "          lc_start_date TYPE ty_start_date VALUE '20220312'." 
				+ LINE_SEP 
				+ LINE_SEP + "    \" if only a single declaration (or a very small ratio of them) has a VALUE, a comment, etc.," 
				+ LINE_SEP + "    \" no extra column is created for it" 
				+ LINE_SEP + "    DATA lth_any_table TYPE        ty_th_hash_table_type. \" only line with a comment" 
				+ LINE_SEP + "    DATA lo_contract TYPE REF TO cl_contract  ##NEEDED." 
				+ LINE_SEP + "    DATA      ls_item_data  TYPE if_any_interface=>ty_s_item." 
				+ LINE_SEP + "    DATA lv_was_saved TYPE abap_bool VALUE abap_false ##NO_TEXT." 
				+ LINE_SEP 
				+ LINE_SEP + "    FIELD-SYMBOLS: " 
				+ LINE_SEP + "         <ls_data> TYPE ty_s_data, \" first comment line" 
				+ LINE_SEP + "      <ls_amount> LIKE LINE OF its_amount," 
				+ LINE_SEP + "    <ls_contract> TYPE ty_s_contract, \" second comment line" 
				+ LINE_SEP + "   <ls_param> LIKE LINE OF mt_parameter." 
				+ LINE_SEP 
				+ LINE_SEP + "    TYPES:"
				+ LINE_SEP + "      BEGIN OF ty_s_outer,"
				+ LINE_SEP + "      one TYPE i,"
				+ LINE_SEP + "      two TYPE i,"
				+ LINE_SEP + "      BEGIN OF ty_s_inner,"
				+ LINE_SEP + "      a1 TYPE i,"
				+ LINE_SEP + "      b2 TYPE i,"
				+ LINE_SEP + "      c3 TYPE i,"
				+ LINE_SEP + "      END OF ty_s_inner,"
				+ LINE_SEP + "      three TYPE i,"
				+ LINE_SEP + "      four TYPE i,"
				+ LINE_SEP + "      BEGIN OF ty_s_another_inner,"
				+ LINE_SEP + "      long_component_name TYPE i,"
				+ LINE_SEP + "      very_long_component_name TYPE i,"
				+ LINE_SEP + "      END OF ty_s_another_inner,"
				+ LINE_SEP + "      seventeen TYPE i,"
				+ LINE_SEP + "      eighteen TYPE i,"
				+ LINE_SEP + "      END OF ty_s_outer,"
				+ LINE_SEP + "      ty_tt_outer TYPE STANDARD TABLE OF ty_s_outer WITH DEFAULT KEY,"
				+ LINE_SEP 
				+ LINE_SEP + "      BEGIN OF ty_s_outer_2,"
				+ LINE_SEP + "      any_component TYPE i,"
				+ LINE_SEP + "      BEGIN OF ty_s_inner,"
				+ LINE_SEP + "      alpha TYPE i,"
				+ LINE_SEP + "      beta TYPE i,"
				+ LINE_SEP + "      END OF ty_s_inner,"
				+ LINE_SEP + "      other_component TYPE i,"
				+ LINE_SEP + "      END OF ty_s_outer_2."
				+ LINE_SEP 
				+ LINE_SEP + "    \" alignment across comments and empty lines (depending on configuration):" 
				+ LINE_SEP + "    DATA lv_value TYPE i." 
				+ LINE_SEP + "    \" comment" 
				+ LINE_SEP + "    DATA lv_long_variable_name TYPE string." 
				+ LINE_SEP 
				+ LINE_SEP + "    DATA lts_sorted_table LIKE its_table." 
				+ LINE_SEP + "  ENDMETHOD.";
   }

	final ConfigBoolValue configExecuteOnClassDefinitionSections = new ConfigBoolValue(this, "ExecuteOnClassDefinitionSections", "Execute on CLASS ... DEFINITION sections", true);
	final ConfigIntValue configFillPercentageToJustifyOwnColumn = new ConfigIntValue(this, "FillPercentageToJustifyOwnColumn", "Fill Ratio to justify own column", "%", 1, 20, 100);
	final ConfigEnumValue<StructureAlignStyle> configStructureAlignStyle = new ConfigEnumValue<StructureAlignStyle>(this, "StructureAlignStyle", "Alignment of nested structures:", new String[] { "align outer structure with inner", "align outer structure independently (like Pretty Printer)", "align each section independently" }, StructureAlignStyle.PER_LEVEL, StructureAlignStyle.ACROSS_LEVELS, LocalDate.of(2023, 5, 21) ); 

	private final ConfigValue[] configValues = new ConfigValue[] { configExecuteOnClassDefinitionSections, configAlignAcrossEmptyLines, configAlignAcrossCommentLines, configFillPercentageToJustifyOwnColumn, configStructureAlignStyle };

	@Override
	public ConfigValue[] getConfigValues() { return configValues; }

	public AlignDeclarationsRule(Profile profile) {
		super(profile);
		initializeConfiguration();
	}

	@Override
	protected boolean isMatchForFirstCommand(Command command, int pass) {
		return command.isDeclaration() && (configExecuteOnClassDefinitionSections.getValue() || !command.isInClassDefinition());
	}

	@Override
	protected boolean isMatchForFurtherCommand(Command command, String keywordOfFirstCommand, int pass) {
		if (command.getFirstToken().isAnyKeyword(keywordOfFirstCommand))
			return command.getFirstToken().getNext() != null && !command.getFirstToken().getNext().isChainColon();
		else if (AbapCult.stringEquals(keywordOfFirstCommand, "TYPES", true) && command.isDeclarationInclude())
			return true;
		else
			return false;
	}

	@Override
	protected void executeOn(Code code, Command startCommand, Command endCommand, boolean startCommandIsChain, int pass) throws UnexpectedSyntaxBeforeChanges {
		// if (startCommand.containsInnerCommentLine())
		// 	return;

		boolean includeKeywordInTable = includeKeywordInTable(startCommand, endCommand);

		// determine the basic indent of the sequence and whether to start with line breaks
		TableStart tableStart = determineTableStart(startCommand, endCommand, includeKeywordInTable);

		// build the tables (note that BEGIN OF always starts a new table at the next level, while END OF resumes the table from the previous level) 
		TableSet tableSet;
		try {
			tableSet = buildTable(startCommand, endCommand, includeKeywordInTable);
			if (tableSet == null)
				return;
		} catch (UnexpectedSyntaxException ex) {
			throw new UnexpectedSyntaxBeforeChanges(this, ex);
		}

		// align the tables
		int firstLineBreaks = tableStart.firstLineBreaks;
		for (AlignTable table : tableSet.getAllTables()) { 
			if (table.getLineCount() == 0) 
				continue;

			// determine indents for this table, which may or may not contain a keyword column
			AlignColumn keywordColumn = table.getColumn(Columns.KEYWORD.getValue());
			int basicIndent = keywordColumn.isEmpty()? tableStart.basicIdentifierIndent : tableStart.keywordIndent;

			// if the keywords get an own column:
			if (includeKeywordInTable) {
				// ensure that all keywords have the required width (esp. 'TYPES' if there is 'TYPES:' in another AlignTable)
				keywordColumn.setMinimumWidth(tableStart.basicIdentifierIndent - tableStart.keywordIndent - 1);
				
				// after a keyword, continue with the next Token (which may be a comment) 
				for (int line = 0; line < table.getLineCount(); ++line) {
					AlignCell keywordCell = table.getLine(line).getCell(keywordColumn);
					if (keywordCell != null) {
						Token next = keywordCell.getLastToken().getNext();
						if (!next.isAsteriskCommentLine() && next.lineBreaks > 0) {
							next.setWhitespace();
							code.addRuleUse(this, next.getParentCommand());
						}
					}
				}
			}
			
			Command[] changedCommands = table.align(basicIndent, firstLineBreaks, false);
			code.addRuleUses(this, changedCommands);
			
			firstLineBreaks = 1;
		}
		
		alignInnerCommentLines(startCommand, endCommand);
	}

	private boolean includeKeywordInTable(Command startCommand, Command endCommand) {
		// determine
		// - whether there is a chain in the Command range
		// - whether any of the declarations continues directly (i.e. without line break) behind the keyword
		//   (esp. "TYPES ..." or "TYPES: ...") or whether all Commands have line breaks after the keyword; 
		boolean hasAnyChain = false;
		boolean continuesBehindAnyKeyword = false;
		Command command = startCommand;
		String keywordOfFirstCommand = startCommand.getFirstToken().getText();
		while (command != endCommand) {
			if (!command.isCommentLine() && command.getFirstToken().isAnyKeyword(keywordOfFirstCommand)) {
				Token identifier = command.getFirstToken().getNextNonCommentSibling();
				if (identifier.isChainColon()) {
					identifier = identifier.getNextNonCommentSibling();
					hasAnyChain = true;
				}
				if (identifier.lineBreaks == 0) {
					continuesBehindAnyKeyword = true;
				}
			}
			command = command.getNext();
		}
		return continuesBehindAnyKeyword || !hasAnyChain;
	}
	
	private TableStart determineTableStart(Command startCommand, Command endCommand, boolean includeKeywordInTable) {
		Token firstToken = startCommand.getFirstToken();
		int firstLineBreaks;
		int keywordIndent = firstToken.getStartIndexInLine();
		int basicIdentifierIndent;
		
		if (includeKeywordInTable) {
			// the first keyword of each Command will be included in the table, too
			firstLineBreaks = firstToken.lineBreaks;
			basicIdentifierIndent = firstToken.getEndIndexInLine() + 1;

			// consider chain colons in the basic indent of the identifiers   
			Command command = startCommand;
			String keywordOfFirstCommand = firstToken.getText();
			while (command != endCommand) {
				if (command.getFirstToken().isAnyKeyword(keywordOfFirstCommand) && command.isSimpleChain()) {
					Token colon = command.getFirstToken().getNextNonCommentSibling();
					if (colon.isChainColon() && colon.lineBreaks == 0) {
						basicIdentifierIndent = Math.max(basicIdentifierIndent, colon.getEndIndexInLine() + 1);
					}
				}
				command = command.getNext();
			}

		} else {
			// the first keyword of each Commands will NOT be considered in the table, i.e. alignment is done for the 
			// content AFTER the first keyword, possibly keeping a "KEYWORD:<line break>" scenario; 
			// this branch is selected if there is a line break after each keyword, and at least one chain is involved 
			if (startCommand.isSimpleChain()) {
				Token colon = firstToken.getNextNonCommentToken();
				Token offsetToken = colon.getNextNonCommentToken();
				firstLineBreaks = offsetToken.lineBreaks;
				if (firstLineBreaks > 0) {
					basicIdentifierIndent = firstToken.getStartIndexInLine() + ABAP.INDENT_STEP;
				} else {
					// currently impossible to be here, but in case logic is changed later
					basicIdentifierIndent = colon.getEndIndexInLine() + 1; // not "offsetToken.getStartIndexInLine()", as this is often weirdly indented
				}
			} else {
				Token offsetToken = firstToken.getNextNonCommentToken();
				firstLineBreaks = offsetToken.lineBreaks;
				basicIdentifierIndent = offsetToken.getStartIndexInLine();
			}
		}
		return new TableStart(firstLineBreaks, keywordIndent, basicIdentifierIndent);
	}

	private TableSet buildTable(Command startCommand, Command endCommand, boolean includeKeywordInTable) throws UnexpectedSyntaxException {
		TableSet tableSet = new TableSet(StructureAlignStyle.forValue(configStructureAlignStyle.getValue()));

		// for lines within (possibly nested) BEGIN OF ... END OF blocks, an additionalIndent is used
		Command command = startCommand;
		Token token = command.getFirstToken();
		
		do {
			// token is now 
			// - either the very first token of a new declaration command (chain or non-chain) - i.e. the declaration keyword DATA, TYPES, etc.
			// - or the first Token after the , in a declaration chain (i.e. an identifier, "BEGIN OF", "END OF" etc.)
			
			if (command.isSimpleChain()) {
				// skip comment lines between chain elements 
				while (token != null && token.isCommentLine()) {
					token = token.getNext();
				}
				if (token == null)
					break;
			} else {
				// skip Commands that are whole comment lines 
				while (command.isCommentLine() && command != endCommand)
					command = command.getNext();
				if (command == endCommand)
					break;
				commandForErrorMsg = command;
				token = command.getFirstToken();
			}

			// if a section is closed with "END OF", then end the current table now, so the AlignLine that contains "END OF" already belongs to the table from the previous level
			if (endOfStructureReached(token)) {
				tableSet.endOfStructure();
			}

			AlignLine line = tableSet.getCurrentTable().addLine();

			// keyword DATA, FIELD-SYMBOLS or TYPES, possibly with ":"
			if (token.isAnyKeyword(Command.declarationKeywords)) { 
				token = readDeclarationKeyword(line, token, includeKeywordInTable);
			}

			// (structure) declaration line
			if (token.matchesOnSiblings(true, "BEGIN|END", "OF") || token.matchesOnSiblings(true, "INCLUDE", "TYPE|STRUCTURE")) {
				token = readStructureDeclaration(line, token, tableSet);
			} else {
				token = readDeclarationLine(line, token, tableSet.additionalIndent);
			}
			if (token == null)
				return null;

			// comment at line end (except in BEGIN OF / END OF / INCLUDE TYPE ... lines, where the line-end comment was already skipped on purpose)
			token = readCommentAtLineEnd(line, token);

			token = token.getNext();
			if (command.isSimpleChain() && token != null) {
				// continue
			} else {
				command = command.getNext();
				if (command == endCommand)
					break;
				if (command.containsInnerCommentLine())
					return null;
				commandForErrorMsg = command;
				token = command.getFirstToken();
			}
		} while (true);
		
		for (AlignTable table : tableSet.getAllTables()) {
			joinSparselyFilledColumns(table);
		}
		
		return tableSet;
	}

	private boolean endOfStructureReached(Token token) {
		Token testToken = token;

		// skip declaration keyword (if any) and chain colon 
		if (testToken.isAnyKeyword(Command.declarationKeywords)) {
			testToken = testToken.getNextNonCommentSibling();
			if (testToken.isChainColon())
				testToken = testToken.getNextNonCommentSibling();
		}
		
		return testToken.matchesOnSiblings(true, "END", "OF");
	}

	private Token readDeclarationKeyword(AlignLine line, Token token, boolean includeKeywordInTable) throws UnexpectedSyntaxException {
		// the keywords (and the colons following them, if any) are included in the table in cases where 
		// the remaining content shall be aligned behind (not below) the keywords
		Command command = token.getParentCommand();
		if (includeKeywordInTable) {
			AlignCell newCell;
			if (command.isSimpleChain()) {
				Token colon = token.getNextNonCommentSibling();
				newCell = new AlignCellTerm(Term.createForTokenRange(token, colon));
			} else {
				newCell = new AlignCellToken(token);
			}
			line.setCell(Columns.KEYWORD.getValue(), newCell);
		}
		// move behind the token (and the chain colon, if any)
		if (command.isSimpleChain())
			token = token.getNextNonCommentToken(); 
		token = token.getNextNonCommentToken();
		return token;
	}

	private Token readStructureDeclaration(AlignLine line, Token token, TableSet tableSet) {
		// treat the first keyword like an identifier, so it is aligned like the other component names; 
		// however, override the text width to be just 1 character wide (i.e. don't move the TYPE column in the component lines because of this) 
		AlignCell newCell = AlignCellToken.createSpecial(token, tableSet.additionalIndent, true);
		line.setCell(Columns.IDENTIFIER.getValue(), newCell);
		
		if (token.matchesOnSiblings(true, "BEGIN", "OF")) {
			// start a new AlignTable for the next AlignLine
			tableSet.beginOfStructure();
		}
		
		// skip the rest of the line, including a line-end comment
		token = token.getLastTokenOnSiblings(true, TokenSearch.ASTERISK, ",|.");
		if (token == null)
			return null;
		if (token.getNext() != null && token.getNext().isCommentAfterCode())
			token = token.getNext();
		return token;
	}

	private Token readDeclarationLine(AlignLine line, Token token, int additionalIndent) throws UnexpectedSyntaxException {
		// identifier; may have the form "var(len)", therefore consider it as a Term
		Term identifier = Term.createSimple(token);
		AlignCell newCell = AlignCellTerm.createSpecial(identifier, additionalIndent, false);
		line.setCell(Columns.IDENTIFIER.getValue(), newCell);

		token = identifier.getNext();
		if (token.isCommaOrPeriod()) // e.g., "DATA: lv_text(100), ..." uses the default type c  
			return token;
		if (!token.isAnyKeyword("TYPE", "LIKE"))
			return null;
		
		// TYPE|LIKE [{LINE OF}|{RANGE OF}|{REF TO}|{{STANDARD|SORTED|HASHED} TABLE OF [REF TO]}] <identifier>
		// (not specifically considered here, but probably irrelevant, as they are skipped): [tabkeys], [INITIAL SIZE n] [WITH HEADER LINE]
		Token typeStart = token;
		Token typeEnd = token;
		while (typeEnd != null && !typeEnd.isAnyKeyword("LENGTH", "DECIMALS", "VALUE", "READ-ONLY") && !typeEnd.textEqualsAny(".", ",")) {
			// do not align table declarations with "WITH ... KEY ..." sections, because they usually should not be put on a single line; 
			// however, do accept the short cases of "WITH EMPTY KEY" and "WITH [UNIQUE | NON-UNIQUE] DEFAULT KEY"
			if (typeEnd.isKeyword("WITH") && !typeEnd.matchesOnSiblings(true, "WITH", "EMPTY", "KEY") 
					&& !typeEnd.matchesOnSiblings(true, "WITH", TokenSearch.makeOptional("UNIQUE|NON-UNIQUE"), "DEFAULT", "KEY")) {
				return null;
			}
			typeEnd = typeEnd.getNextSibling();
		}
		if (typeEnd == null)
			return null;
		Term typeInfo = Term.createForTokenRange(typeStart, typeEnd.getPrev());
		line.setCell(Columns.TYPE.getValue(), new AlignCellTerm(typeInfo));
		token = typeEnd;

		// the remaining components only appear in DATA and TYPES, not with FIELD-SYMBOLS
		if (token.isCommaOrPeriod())
			return token;

		// [LENGTH <identifier>]
		Token lengthInfoLast = token.getLastTokenOnSiblings(true, "LENGTH", TokenSearch.ANY_IDENTIFIER_OR_LITERAL);
		if (lengthInfoLast != null) {
			line.setCell(Columns.LENGTH.getValue(), new AlignCellTerm(Term.createForTokenRange(token, lengthInfoLast)));
			token = lengthInfoLast.getNext();
		}

		// [DECIMALS <identifier>]
		Token decimalsInfoLast = token.getLastTokenOnSiblings(true, "DECIMALS", TokenSearch.ANY_IDENTIFIER_OR_LITERAL);
		if (decimalsInfoLast != null) {
			line.setCell(Columns.DECIMALS.getValue(), new AlignCellTerm(Term.createForTokenRange(token, decimalsInfoLast)));
			token = decimalsInfoLast.getNext();
		}

		// [VALUE <term>|{IS INITIAL}]
		if (token.isKeyword("VALUE")) {
			Token valueLast = token.getLastTokenOnSiblings(true, "VALUE", "IS", "INITIAL");
			if (valueLast == null) {
				Term valueTerm = Term.createSimple(token.getNext());
				valueLast = valueTerm.lastToken;
			}
			if (valueLast == null)
				return null;
			line.setCell(Columns.VALUE.getValue(), new AlignCellTerm(Term.createForTokenRange(token, valueLast)));
			token = valueLast.getNext();
		}

		// [READ-ONLY]
		if (token.isKeyword("READ-ONLY")) {
			line.setCell(Columns.READ_ONLY.getValue(), new AlignCellToken(token));
			token = token.getNext();
		}

		// . or , (may be preceded by a pragma)
		token = token.getLastTokenOnSiblings(true, TokenSearch.ASTERISK, ",|.");
		if (token == null)
			return null;

		return token;
	}

	private Token readCommentAtLineEnd(AlignLine line, Token token) {
		if (token.getNext() != null && token.getNext().isCommentAfterCode()) {
			token = token.getNext();
			line.setCell(Columns.LINE_END_COMMENT.getValue(), new AlignCellToken(token));
		}
		return token;
	}

	private void joinSparselyFilledColumns(AlignTable table) throws UnexpectedSyntaxException {
		// decide whether rarely used columns should really be aligned (i.e. whether horizontal space should be reserved for them):
		// if only one single line (or less than 20% of all lines) have content in this column, then join the content into a previous column

		double fillRatioToJustifyOwnColumn = configFillPercentageToJustifyOwnColumn.getValue() / 100.0;

		for (int i = Columns.LINE_END_COMMENT.getValue(); i >= Columns.LENGTH.getValue(); --i) {
			AlignColumn testColumn = table.getColumn(i);
			if (testColumn.isEmpty())
				continue;
			if (testColumn.getCellCount() <= 1 || testColumn.getCellCount() < (int) (table.getLineCount() * fillRatioToJustifyOwnColumn))
				testColumn.joinIntoPreviousColumns();
		}

		// if LENGTH is always preceded by TYPE sections with the same length ("TYPE c", "TYPE p" etc.), then join it into the TYPE column 
		AlignColumn lengthColumn = table.getColumn(Columns.LENGTH.getValue()); 
		if (lengthColumn.isPreviousColumnFixedWidth()) {
			lengthColumn.joinIntoPreviousColumns();
		}
	}

	private void alignInnerCommentLines(Command startCommand, Command endCommand) {
		Command command = startCommand;
		int blockLevel = 0;
		while (command != endCommand) {
			blockLevel += command.getBlockLevelDiff();
			Token token = command.getFirstToken();
			while (token != null) {
				if (token.isQuotMarkCommentLine()) {
					Token nextCode = token.getNextNonCommentToken();
					if (nextCode != null) {
						// align with next code, but if next code reads "END OF", add an indent step
						token.spacesLeft = nextCode.spacesLeft + (nextCode.matchesOnSiblings(true, "END", "OF") ? ABAP.INDENT_STEP : 0);
					} else if (blockLevel > 0) {
						// inside a BEGIN OF block, align with the next executable code line
						Command nextCodeCommand = command.getNextNonCommentCommand();
						if (nextCodeCommand != null)
							token.spacesLeft = nextCodeCommand.getFirstToken().spacesLeft;
					}
				}
				token = token.getNext();
			}
			command = command.getNext();
		}
	}
}
