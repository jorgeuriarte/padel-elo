def data = [
    pA:[points: 2000],
    pB:[points: 2000],
    pC:[points: 2000],
    pD:[points: 2000],
    pE:[points: 2000],
    pF:[points: 2000]
    ]

def playerPoints(player, data) {
    data.find { it.key == player }?.value?.points?:0
}

def K = 20

def matchs = [
    [date:'20160101', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: 1],
    [date:'20160102', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: -1],
    [date:'20160103', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: 2],
    [date:'20160103', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: -2],
    [date:'2016010X', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: 1],
    [date:'20160103', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: 2],
    [date:'20160104', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: -2],
    [date:'20160101', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: 2],
    [date:'20160101', teamA: ['pA', 'pB'], teamB: ['pC', 'pD'], result: 2],
    [date:'20160101', teamA: ['pA', 'pE'], teamB: ['pC', 'pD'], result: 2],
    [date:'20160101', teamA: ['pA', 'pE'], teamB: ['pC', 'pD'], result: 2]
]

def playerSets(player, sets) {
    sets.findAll { (it.win + it.vs).contains(player) }
}

def virtualOpponentScore(mate, opponents, data) {
    opponents.collect { playerPoints(it, data) }.sum() - playerPoints(mate, data)
}

def expected(playerScore, opponentScore) {
    1/(1+10.power((opponentScore-playerScore)/400))
}

def playerMate(player, match) {
    if (player in match.teamA)
        return (match.teamA - player)[0]
    if (player in match.teamB)
        return (match.teamB - player)[0]
}

def opponentTeam(player, match) {
    if (player in match.teamA)
        return match.teamB
    if (player in match.teamB)
        return match.teamA
}

def playerResult(player, match) {
    if (player in match.teamA)
        return match.result
    if (player in match.teamB)
        return -1 * match.result
}

def changes = [:].withDefault{ key -> 0 }
matchs.sort { e1, e2 -> e1.date <=> e2.date }.each { m ->
    (m.teamA + m.teamB).each { player ->
        def mate = playerMate(player, m)
        def opponents = opponentTeam(player, m)
        def points = playerPoints(player, data)
        def opponentScore = virtualOpponentScore(mate,opponents, data)
        def expected = expected(points, opponentScore)
        def playerChange = 0
        (0..Math.abs(m.result)-1).each { set ->
            result = (playerResult(player, m) > 0) ? 1 : 0
            playerChange = K * (result - expected)
            changes[player] += playerChange
        }
    }
}

void updateRanking(data, changes) {
    data.each { player, info ->
        data[player].points+=changes[player]
    }
}
updateRanking(data, changes)
println data
