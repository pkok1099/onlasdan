package comic.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val CardCornerRadius = 16.dp
val SheetCornerRadius = 20.dp
val ChipCornerRadius = 12.dp
val ButtonCornerRadius = 16.dp
val SmallButtonCornerRadius = 12.dp

val CardShape = RoundedCornerShape(CardCornerRadius)
val SheetShape = RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)
val FullSheetShape = RoundedCornerShape(SheetCornerRadius)
val ChipShape = RoundedCornerShape(ChipCornerRadius)
val ButtonShape = RoundedCornerShape(ButtonCornerRadius)
val SmallButtonShape = RoundedCornerShape(SmallButtonCornerRadius)
val ComicStampShape = RoundedCornerShape(6.dp)
val ComicBurstBubbleShape = RoundedCornerShape(18.dp)
val PillShape = RoundedCornerShape(percent = 50)

val NoirShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
