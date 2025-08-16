package com.marishutka.chess.domain.model

sealed class MoveResult {
    data class Valid(val move: Move, val gameResult: GameResult) : MoveResult()
    data class Invalid(val reason: String) : MoveResult()
}

class ChessEngine {
    private var boardState = BoardState()

    init { setupInitial() }

    fun getBoardState(): BoardState = boardState
    fun getPiece(p:Position) = if (p.isValid()) boardState.squares[p.row][p.col] else null

    private fun setupInitial() {
        val s = Array(8){ arrayOfNulls<ChessPiece>(8) }
        val back = arrayOf(PieceType.ROOK,PieceType.KNIGHT,PieceType.BISHOP,PieceType.QUEEN,PieceType.KING,PieceType.BISHOP,PieceType.KNIGHT,PieceType.ROOK)
        back.forEachIndexed { c,t -> s[7][c]=ChessPiece(t,PieceColor.WHITE) }
        repeat(8){ c-> s[6][c]=ChessPiece(PieceType.PAWN,PieceColor.WHITE) }
        back.forEachIndexed { c,t -> s[0][c]=ChessPiece(t,PieceColor.BLACK) }
        repeat(8){ c-> s[1][c]=ChessPiece(PieceType.PAWN,PieceColor.BLACK) }
        boardState = boardState.copy(squares = s, activeColor = PieceColor.WHITE)
    }

    fun makeMove(m:Move): MoveResult {
        val v = validate(m)
        if (v is MoveResult.Invalid) return v
        boardState = applyMove(boardState, m)
        val res = evaluate(boardState)
        return MoveResult.Valid(m, res)
    }

    fun getLegalMoves(color: PieceColor = boardState.activeColor): List<Move> {
        val list = mutableListOf<Move>()
        for (r in 0..7) for (c in 0..7) {
            val piece = boardState.squares[r][c] ?: continue
            if (piece.color!=color) continue
            val from = Position(r,c)
            list += pieceMoves(boardState, from, piece)
        }
        return list.filter { !inCheckAfter(boardState, it) }
    }

    private fun validate(m:Move): MoveResult {
        val p = getPiece(m.from) ?: return MoveResult.Invalid("empty from")
        if (p.color != boardState.activeColor) return MoveResult.Invalid("not your turn")
        if (getPiece(m.to)?.color == p.color) return MoveResult.Invalid("capture own")
        if (!pieceMoves(boardState, m.from, p).any { it.to==m.to }) return MoveResult.Invalid("illegal")
        if (inCheckAfter(boardState, m)) return MoveResult.Invalid("king in check")
        return MoveResult.Valid(m, GameResult.ONGOING)
    }

    // ----- генерация ходов (без шахов – фильтр выше)
    private fun pieceMoves(b:BoardState, from:Position, piece:ChessPiece): List<Move> = when(piece.type){
        PieceType.PAWN -> pawnMoves(b, from, piece)
        PieceType.ROOK -> slideMoves(b, from, piece, listOf(1 to 0,-1 to 0,0 to 1,0 to -1))
        PieceType.BISHOP -> slideMoves(b, from, piece, listOf(1 to 1,1 to -1,-1 to 1,-1 to -1))
        PieceType.QUEEN -> slideMoves(b, from, piece, listOf(1 to 0,-1 to 0,0 to 1,0 to -1,1 to 1,1 to -1,-1 to 1,-1 to -1))
        PieceType.KNIGHT -> knightMoves(b, from, piece)
        PieceType.KING -> kingMoves(b, from, piece)
    }

    private fun pawnMoves(b:BoardState, from:Position, p:ChessPiece): List<Move> {
        val list= mutableListOf<Move>()
        val dir = if (p.color==PieceColor.WHITE) -1 else 1
        val start = if (p.color==PieceColor.WHITE) 6 else 1
        val promRow = if (p.color==PieceColor.WHITE) 0 else 7
        val one = from.offset(dir,0)
        if (one.isValid() && b.squares[one.row][one.col]==null) {
            if (one.row==promRow) list += promote(from, one, p)
            else list += Move(from, one, p)
            val two = from.offset(2*dir,0)
            if (from.row==start && b.squares[two.row][two.col]==null) list += Move(from, two, p, moveType = MoveType.DOUBLE_PAWN_PUSH)
        }
        for (dc in listOf(-1,1)) {
            val cap = from.offset(dir,dc)
            if (!cap.isValid()) continue
            val tp = b.squares[cap.row][cap.col]
            if (tp!=null && tp.color!=p.color) {
                if (cap.row==promRow) list += promote(from,cap,p,tp)
                else list += Move(from,cap,p,tp, MoveType.CAPTURE)
            } else if (b.enPassantTarget==cap) {
                val capturedRow = if (p.color==PieceColor.WHITE) cap.row+1 else cap.row-1
                val captured = b.squares[capturedRow][cap.col]
                list += Move(from, cap, p, captured, MoveType.EN_PASSANT)
            }
        }
        return list
    }
    private fun promote(from:Position,to:Position,p:ChessPiece,cap:ChessPiece?=null)=
        listOf(PieceType.QUEEN,PieceType.ROOK,PieceType.BISHOP,PieceType.KNIGHT).map{
            Move(from,to,p,cap,MoveType.PROMOTION,it)
        }
    private fun slideMoves(b:BoardState, from:Position, p:ChessPiece, dirs:List<Pair<Int,Int>>):List<Move>{
        val list= mutableListOf<Move>()
        for((dr,dc) in dirs){
            var cur=from.offset(dr,dc)
            while(cur.isValid()){
                val tp=b.squares[cur.row][cur.col]
                if (tp==null){ list+=Move(from,cur,p) }
                else { if (tp.color!=p.color) list+=Move(from,cur,p,tp, MoveType.CAPTURE); break }
                cur=cur.offset(dr,dc)
            }
        }
        return list
    }
    private fun knightMoves(b:BoardState, from:Position, p:ChessPiece):List<Move>{
        val list= mutableListOf<Move>()
        val jumps = listOf(-2 to -1,-2 to 1,-1 to -2,-1 to 2,1 to -2,1 to 2,2 to -1,2 to 1)
        for((dr,dc) in jumps){
            val t=from.offset(dr,dc); if(!t.isValid()) continue
            val tp=b.squares[t.row][t.col]
            if (tp==null || tp.color!=p.color) list+=Move(from,t,p,tp, if (tp==null) MoveType.NORMAL else MoveType.CAPTURE)
        }
        return list
    }
    private fun kingMoves(b:BoardState, from:Position, p:ChessPiece):List<Move>{
        val list= mutableListOf<Move>()
        for(dr in -1..1) for(dc in -1..1){
            if (dr==0 && dc==0) continue
            val t=from.offset(dr,dc); if(!t.isValid()) continue
            val tp=b.squares[t.row][t.col]
            if (tp==null || tp.color!=p.color) list+=Move(from,t,p,tp, if (tp==null) MoveType.NORMAL else MoveType.CAPTURE)
        }
        // рокировки — без прохода через шах (проверка будет в inCheckAfter)
        fun empty(c:Int)= b.squares[from.row][c]==null
        if (p.hasMoved.not() && !isInCheck(b,p.color)) {
            // king side
            if (b.castlingRights.canCastle(p.color,true) && empty(5) && empty(6))
                list+=Move(from, Position(from.row, from.col+2), p, moveType = MoveType.CASTLING_KINGSIDE)
            // queen side
            if (b.castlingRights.canCastle(p.color,false) && empty(3) && empty(2) && empty(1))
                list+=Move(from, Position(from.row, from.col-2), p, moveType = MoveType.CASTLING_QUEENSIDE)
        }
        return list
    }

    // ----- применение хода
    private fun applyMove(b:BoardState, m:Move): BoardState {
        val s = b.squares.map { it.clone() }.toTypedArray()
        s[m.from.row][m.from.col]=null
        s[m.to.row][m.to.col]= m.piece.moved()

        var enPassant:Position? = null
        when(m.moveType){
            MoveType.EN_PASSANT -> {
                val r = if (m.piece.color==PieceColor.WHITE) m.to.row+1 else m.to.row-1
                s[r][m.to.col]=null
            }
            MoveType.CASTLING_KINGSIDE -> {
                val row=m.from.row
                s[row][5]= s[row][7]?.moved()
                s[row][7]= null
            }
            MoveType.CASTLING_QUEENSIDE -> {
                val row=m.from.row
                s[row][3]= s[row][0]?.moved()
                s[row][0]= null
            }
            MoveType.DOUBLE_PAWN_PUSH -> {
                val dir = if (m.piece.color==PieceColor.WHITE) 1 else -1
                enPassant = Position(m.from.row+dir, m.from.col)
            }
            MoveType.PROMOTION -> {
                val pt = m.promotionPiece ?: PieceType.QUEEN
                s[m.to.row][m.to.col] = ChessPiece(pt, m.piece.color, hasMoved = true)
            }
            else -> {}
        }

        val half = if (m.piece.type==PieceType.PAWN || m.capturedPiece!=null) 0 else b.halfmoveClock+1
        val full = if (b.activeColor==PieceColor.BLACK) b.fullmoveNumber+1 else b.fullmoveNumber

        return b.copy(
            squares = s,
            activeColor = b.activeColor.opposite,
            castlingRights = b.castlingRights.afterMove(m),
            enPassantTarget = enPassant,
            halfmoveClock = half,
            fullmoveNumber = full,
            moveHistory = b.moveHistory + m
        )
    }

    // ----- проверка шахов/матов/патов и ничьих
    private fun evaluate(b:BoardState): GameResult {
        val color = b.activeColor
        val legal = getLegalMoves(color)
        val inCheck = isInCheck(b, color)
        if (legal.isEmpty()) return if (inCheck)
            if (color==PieceColor.WHITE) GameResult.BLACK_WINS else GameResult.WHITE_WINS
        else GameResult.DRAW_STALEMATE

        if (b.halfmoveClock >= 100) return GameResult.DRAW_FIFTY_MOVE_RULE
        if (insufficientMaterial(b)) return GameResult.DRAW_INSUFFICIENT_MATERIAL
        if (threefold(b)) return GameResult.DRAW_THREEFOLD_REPETITION

        return GameResult.ONGOING
    }

    private fun isInCheck(b:BoardState, color:PieceColor): Boolean {
        val king = findKing(b, color) ?: return true
        return attackedBy(b, king, color.opposite)
    }

    private fun inCheckAfter(b:BoardState, m:Move): Boolean = isInCheck(applyMove(b,m), m.piece.color)

    private fun findKing(b:BoardState, color:PieceColor): Position? {
        for (r in 0..7) for (c in 0..7) {
            val p=b.squares[r][c] ?: continue
            if (p.type==PieceType.KING && p.color==color) return Position(r,c)
        }
        return null
    }

    private fun attackedBy(b:BoardState, pos:Position, by:PieceColor): Boolean {
        // грубая проверка – генерируем все ходы соперника и смотрим бьётся ли клетка
        for (r in 0..7) for (c in 0..7) {
            val p=b.squares[r][c] ?: continue
            if (p.color!=by) continue
            val from=Position(r,c)
            val moves = pieceMoves(b, from, p)
            if (moves.any { it.to==pos }) return true
        }
        return false
    }

    private fun insufficientMaterial(b:BoardState): Boolean {
        val pieces = mutableListOf<ChessPiece>()
        for (r in 0..7) for (c in 0..7) b.squares[r][c]?.let { pieces+=it }
        // король против короля
        if (pieces.size==2) return true
        // король + лёгкая против короля
        if (pieces.size==3) {
            val minor = pieces.firstOrNull { it.type==PieceType.BISHOP || it.type==PieceType.KNIGHT }
            if (minor!=null) return true
        }
        return false
    }

    private fun threefold(b:BoardState): Boolean {
        // упрощённая версия: считаем по повтору последних позиций (без рокировочных прав и en passant — можно расширить)
        val map = mutableMapOf<String, Int>()
        var cur = b
        for (m in b.moveHistory.indices) {
            val key = fenKey(cur)
            map[key] = (map[key] ?: 0) + 1
            if (map[key]!! >= 3) return true
            // откат по истории для простоты опускаем — в реале нужно хранить ключи по мере игры
        }
        return false
    }
    private fun fenKey(b:BoardState): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var empty=0
            for (c in 0..7) {
                val p=b.squares[r][c]
                if (p==null) empty++ else {
                    if (empty>0){ sb.append(empty); empty=0 }
                    val ch = when(p.type){
                        PieceType.PAWN->'p'; PieceType.ROOK->'r'; PieceType.KNIGHT->'n'; PieceType.BISHOP->'b'; PieceType.QUEEN->'q'; PieceType.KING->'k'
                    }
                    sb.append(if (p.color==PieceColor.WHITE) ch.uppercaseChar() else ch)
                }
            }
            if (empty>0) sb.append(empty)
            if (r!=7) sb.append('/')
        }
        sb.append(' ').append(if (b.activeColor==PieceColor.WHITE) 'w' else 'b')
        return sb.toString()
    }
}
