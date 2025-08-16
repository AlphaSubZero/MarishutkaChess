package com.marishutka.chess.domain.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import com.marishutka.chess.R

@Serializable
enum class PieceType(val symbol: String, val value: Int) {
    PAWN("P",1), ROOK("R",5), KNIGHT("N",3), BISHOP("B",3), QUEEN("Q",9), KING("K",0)
}
@Serializable enum class PieceColor { WHITE, BLACK; val opposite get() = if (this==WHITE) BLACK else WHITE }

@Serializable
data class ChessPiece(val type: PieceType, val color: PieceColor, val hasMoved:Boolean=false) {
    fun moved() = copy(hasMoved = true)

    fun getDrawableResourceId(): Int {
        // Попытка найти реальную картинку по имени, иначе — заглушка
        val name = "${color.name.lowercase()}_${type.name.lowercase()}"
        return try {
            R.drawable::class.java.getField(name).getInt(null)
        } catch (_: Exception) {
            R.drawable.ic_piece_placeholder
        }
    }
}

@Serializable data class Position(val row:Int, val col:Int) {
    fun isValid() = row in 0..7 && col in 0..7
    fun offset(dr:Int, dc:Int) = Position(row+dr, col+dc)
    fun toAlgebraic() = "${('a'+col)}${8-row}"
    fun distanceTo(o:Position)= maxOf(kotlin.math.abs(row-o.row), kotlin.math.abs(col-o.col))
    fun isOnSameRank(o:Position) = row==o.row
    fun isOnSameFile(o:Position) = col==o.col
    fun isOnSameDiagonal(o:Position)= kotlin.math.abs(row-o.row)==kotlin.math.abs(col-o.col)
    fun direction(to:Position):Pair<Int,Int> {
        val dr = when { to.row>row -> 1; to.row<row -> -1; else -> 0 }
        val dc = when { to.col>col -> 1; to.col<col -> -1; else -> 0 }
        return dr to dc
    }
    companion object { fun fromAlg(s:String)= if (s.length==2) Position(8-(s[1]-'0'), s[0]-'a') else null }
}

@Serializable enum class MoveType { NORMAL, CAPTURE, EN_PASSANT, CASTLING_KINGSIDE, CASTLING_QUEENSIDE, PROMOTION, DOUBLE_PAWN_PUSH }
@Serializable enum class GameResult { WHITE_WINS, BLACK_WINS, DRAW_STALEMATE, DRAW_INSUFFICIENT_MATERIAL, DRAW_THREEFOLD_REPETITION, DRAW_FIFTY_MOVE_RULE, DRAW_AGREEMENT, ONGOING, ABANDONED }

@Serializable
data class Move(
    val from: Position, val to: Position, val piece: ChessPiece,
    val capturedPiece: ChessPiece? = null, val moveType: MoveType = MoveType.NORMAL,
    val promotionPiece: PieceType? = null, val isCheck:Boolean=false, val isCheckmate:Boolean=false,
    val timestamp: Instant = kotlinx.datetime.Clock.System.now()
) {
    fun toUci(): String = "${from.toAlgebraic()}${to.toAlgebraic()}${promotionPiece?.symbol?.lowercase()?:""}"
}

@Serializable
data class GameClock(
    val whiteTimeLeft:Long=600_000, val blackTimeLeft:Long=600_000, val increment:Long=0,
    val isRunning:Boolean=false, val activeColor:PieceColor=PieceColor.WHITE,
    val lastMoveTime: Instant = kotlinx.datetime.Clock.System.now()
)

@Serializable
data class CastlingRights(
    val wK:Boolean=true, val wQ:Boolean=true, val bK:Boolean=true, val bQ:Boolean=true
) {
    fun canCastle(c:PieceColor,kingside:Boolean)= when {
        c==PieceColor.WHITE && kingside -> wK
        c==PieceColor.WHITE && !kingside -> wQ
        c==PieceColor.BLACK && kingside -> bK
        else -> bQ
    }
    fun afterMove(m:Move):CastlingRights {
        var r=this
        if (m.piece.type==PieceType.KING) r= if (m.piece.color==PieceColor.WHITE) r.copy(wK=false,wQ=false) else r.copy(bK=false,bQ=false)
        when (m.from) {
            Position(7,0)-> r=r.copy(wQ=false); Position(7,7)-> r=r.copy(wK=false)
            Position(0,0)-> r=r.copy(bQ=false); Position(0,7)-> r=r.copy(bK=false)
        }
        when (m.to) {
            Position(7,0)-> r=r.copy(wQ=false); Position(7,7)-> r=r.copy(wK=false)
            Position(0,0)-> r=r.copy(bQ=false); Position(0,7)-> r=r.copy(bK=false)
        }
        return r
    }
}

@Serializable
data class BoardState(
    val squares: Array<Array<ChessPiece?>> = Array(8){ arrayOfNulls<ChessPiece>(8) },
    val activeColor: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantTarget: Position? = null,
    val halfmoveClock:Int = 0,
    val fullmoveNumber:Int = 1,
    val moveHistory: List<Move> = emptyList()
)
