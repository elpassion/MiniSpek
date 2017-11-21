package com.elpassion.mspek

import com.elpassion.mspek.MiniSpek.o
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(MiniSpekRunner::class)
class NotSharedStateTest {

    private var i = 1

    @Test
    fun shouldNotShareState() {
        MiniSpek.mspek("some nested test") {
            "1st assertion" o {
                Assert.assertEquals(1, i)
                "nested assertion" o {
                    i++
                    Assert.assertEquals(2, i)
                    "even more nested assertion" o {
                        i++
                        Assert.assertEquals(3, i)
                    }
                }
            }
            "2nd assertion" o {
                i++
                Assert.assertEquals(2, i)
            }
            "3rd assertion" o {
                Assert.assertEquals(1, i)
                i++
            }
        }
    }
}