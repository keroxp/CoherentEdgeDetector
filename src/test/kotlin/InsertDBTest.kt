import ced.CreateDB
import ced.imageFiles
import org.junit.Test

class InsertDBTest {
    @Test fun test () {
        CreateDB.exec(imageFiles("res/test"), "test")
    }
}