package environment.solo.pegsolitaire

import environment.{Board, BoardType}
import environment.BoardType.BoardType
import scalafx.scene.canvas.GraphicsContext

case class PegBoard(grid: List[List[PegCell]], boardType: BoardType) extends Board {
  override def render(gc: GraphicsContext): Unit = {
    for {
      (line, yIndex) <- grid.zipWithIndex
      (cell, xIndex) <- line.zipWithIndex
      _ = cell.render(
        gc,
        cellStartX(line, xIndex),
        cellHeight * yIndex,
        cellWidth,
        cellHeight
      )
    } yield {
      ()
    }
  }

  def cellStartX(line: List[PegCell], xIndex: Int): Int = {
    val baseStartX = cellWidth * xIndex
    boardType match {
      case BoardType.Square => baseStartX
      case BoardType.Triangular =>
        val noneCells = line.count(_.isNone)
        baseStartX + (noneCells * (cellWidth / 2))
      case BoardType.Diamond => baseStartX
      case _ => throw new Exception("Unknown BoardType")
    }
  }
}