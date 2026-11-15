package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
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
    void shouldCreateEntitiesAndParseValidCsvLines() throws InvalidDataException {
        BankBranch branch = BankCsvReader.parseBranchLine("BR-010,Чернівці", 1);
        assertEquals("BR-010", branch.branchCode());
        assertEquals("Чернівці", branch.city());

        Borrower borrower = BankCsvReader.parseBorrowerLine("AA123456,Олена Ковальчук,1995-05-14,35000.0", 2);
        assertEquals("AA123456", borrower.getPassportNum());
        assertEquals(35000.0, borrower.getMonthlyIncome());
    }

    @Test
    void shouldThrowInvalidDataExceptionOnMalformedCsvLines() {
        // Недостатньо колонок у файлі відділень
        assertThrows(InvalidDataException.class,
                () -> BankCsvReader.parseBranchLine("BR-ONLY-CODE", 1));

        // Порожній код відділення (порушення доменної валідації)
        assertThrows(InvalidDataException.class,
                () -> BankCsvReader.parseBranchLine("   ,Київ", 2));

        // Некоректний формат дати у файлі позичальників
        assertThrows(InvalidDataException.class,
                () -> BankCsvReader.parseBorrowerLine("AA111222,Іван,14-05-1995,25000", 3));

        // Позичальник молодший за 21 рік
        assertThrows(InvalidDataException.class,
                () -> BankCsvReader.parseBorrowerLine("AA111222,Іван,2010-01-01,25000", 4));
    }

    @Test
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
        assertTrue(BankUtils.getAvailableAction(RepaymentStatus.OVERDUE).contains("Прострочено"));
    }
}