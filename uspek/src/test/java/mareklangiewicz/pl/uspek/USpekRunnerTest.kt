package mareklangiewicz.pl.uspek

import mareklangiewicz.pl.uspek.USpek.eq
import mareklangiewicz.pl.uspek.USpek.o
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(USpekJUnitRunner::class)
class USpekRunnerTest {

    @Test
    fun some_junit_test() {
        USpek.uspek("some nested test") {
            "1st assertion" o {
                Assert.assertTrue(true)
                "nested assertion" o {
                    Assert.assertTrue(true)
                }
            }
            "2nd assertion" o {
                Assert.assertFalse(false)
                "fjdklfjdkalfjkdasljf" o {
                    1 eq 2
                }
            }
        }
    }
}