import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.ext.web.handler.BodyHandler
import static groovy.json.JsonOutput.toJson
import static groovy.json.JsonOutput.prettyPrint

@groovy.transform.Field def data = [
    pA:[points: 2000],
    pB:[points: 2000],
    pC:[points: 2000],
    pD:[points: 2000],
    pE:[points: 2000],
    pF:[points: 2000]
    ]
@groovy.transform.Field def matchs = [
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

@groovy.transform.Field def K = 20

def playerPoints(player) {
    data.find { it.key == player }?.value?.points?:0
}

def virtualOpponentScore(mate, opponents) {
    opponents.collect { playerPoints(it) }.sum() - playerPoints(mate)
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

void updateRankingWithChanges() {
    def changes = [:].withDefault{ key -> 0 }
    matchs.sort { e1, e2 -> e1.date <=> e2.date }.each { m ->
        (m.teamA + m.teamB).each { player ->
            def mate = playerMate(player, m)
            def opponents = opponentTeam(player, m)
            def points = playerPoints(player)
            def opponentScore = virtualOpponentScore(mate,opponents)
            def expected = expected(points, opponentScore)
            def playerChange = 0
            (0..Math.abs(m.result)-1).each { set ->
                result = (playerResult(player, m) > 0) ? 1 : 0
                playerChange = K * (result - expected)
                changes[player] += playerChange
            }
        }
    }

    data.each { player, info ->
        data[player].points+=changes[player]
    }
}

def showRanking(routingContext) {
  def response = routingContext.response()
  response.end(prettyPrint(toJson(data)))
}

def updateRanking(routingContext) {
  updateRankingWithChanges()
  routingContext.reroute("/ranking/show")
}

def router = Router.router(vertx)

router.route().handler(BodyHandler.create())
router.get("/ranking/show").handler(this.&showRanking)
router.get("/ranking/update").handler(this.&updateRanking)

vertx.createHttpServer().requestHandler(router.&accept).listen(8080)
