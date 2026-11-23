package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.bank.exception.InvalidDataException;
import ua.bank.io.BankCsvReader;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.model.RepaymentStatus;
import ua.bank.util.BankUtils;

class BankLab3Test {

    @Test
    @DisplayName("1. Should parse valid BankBranch CSV line")
    void shouldParseValidBranchCsvLine() throws InvalidDataException {
        BankBranch branch = BankCsvReader.parseBranchLine("  BR-010 , Чернівці  ", 1);

        assertEquals("BR-010", branch.branchCode());
        assertEquals("Чернівці", branch.city());
    }

    @Test
    @DisplayName("2. Should parse valid Borrower CSV line")
    void shouldParseValidBorrowerCsvLine() throws InvalidDataException {
        Borrower borrower = BankCsvReader.parseBorrowerLine("AA123456,Олена Ковальчук,1995-05-14,35000.0", 2);

        assertEquals("AA123456", borrower.getPassportNum());
        assertEquals("Олена Ковальчук", borrower.getFullName());
        assertEquals(35000.0, borrower.getMonthlyIncome());
    }

    @Test
    @DisplayName("3. Should throw InvalidDataException when CSV column count is invalid")
    void shouldThrowInvalidDataExceptionWhenColumnCountIsInvalid() {
        InvalidDataException ex = assertThrows(
                InvalidDataException.class,
                () -> BankCsvReader.parseBranchLine("BR-ONLY-CODE", 4)
        );

        assertTrue(ex.getMessage().contains("#4"), "Exception message must contain line number #4");
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("4. Should throw InvalidDataException with IllegalArgumentException cause when branch code is blank")
    void shouldThrowInvalidDataExceptionWithCauseWhenBranchBlank() {
        InvalidDataException ex = assertThrows(
                InvalidDataException.class,
                () -> BankCsvReader.parseBranchLine("   ,Київ", 7)
        );

        assertTrue(ex.getMessage().contains("#7"), "Exception message must contain line number #7");
        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    @DisplayName("5. Should throw InvalidDataException with DateTimeParseException cause when birth date format is invalid")
    void shouldThrowInvalidDataExceptionWithDateCause() {
        InvalidDataException ex = assertThrows(
                InvalidDataException.class,
                () -> BankCsvReader.parseBorrowerLine("AA111222,Іван Петренко,14-05-1995,25000", 12)
        );

        assertTrue(ex.getMessage().contains("#12"), "Exception message must contain line number #12");
        assertNotNull(ex.getCause());
        assertInstanceOf(DateTimeParseException.class, ex.getCause());
    }

    @Test
    @DisplayName("6. Should throw InvalidDataException with IllegalArgumentException cause when borrower is under 21")
    void shouldThrowInvalidDataExceptionWhenBorrowerUnderage() {
        InvalidDataException ex = assertThrows(
                InvalidDataException.class,
                () -> BankCsvReader.parseBorrowerLine("AA111222,Іван Петренко,2010-01-01,25000", 15)
        );

        assertTrue(ex.getMessage().contains("#15"), "Exception message must contain line number #15");
        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }

    @Test
    @DisplayName("7. Should calculate computed methods and switch expressions in BankUtils")
    void shouldCalculateComputedMethodsAndSwitchExpressionsCorrectly() {
        Borrower borrower = new Borrower("AA123456", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        BankBranch branch = BankBranch.of("BR-010", "Чернівці");
        LoanContract contract = new LoanContract(
                "LN-100", borrower, branch, LoanType.MORTGAGE,
                240000.0, LocalDate.of(2026, 1, 15), 12
        );

        assertEquals(20000.0, BankUtils.monthlyPayment(contract), 0.001);
        assertEquals(LocalDate.of(2027, 1, 15), BankUtils.lastDueDate(contract));
        assertEquals(1.05, BankUtils.getRiskCoefficient(LoanType.MORTGAGE), 0.001);
        assertTrue(BankUtils.getAvailableAction(RepaymentStatus.OVERDUE).contains("Прострочено"),
                "Overdue action description must contain 'Прострочено'");
    }
}