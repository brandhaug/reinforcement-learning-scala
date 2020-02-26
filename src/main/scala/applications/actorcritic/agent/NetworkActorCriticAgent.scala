package applications.actorcritic.agent

import environment.Environment
import applications.actorcritic.Arguments._


case class NetworkActorCriticAgent(initialEnvironment: Environment,
                                   stateActionRewardMap: Map[String, List[ActionReward]] = Map(),
                                   epsilonRate: Double = actorEpsilonRate,
                                   actorEligibilities: Map[String, List[Double]] = Map(),
                                   criticEligibilities: Map[String, Double] = Map(),
                                   stateValueNetwork: StateValueNetwork)
    extends ActorCriticAgent {
  def train(memories: List[Memory]): ActorCriticAgent = {
    val memory = memories.last

    // 1. Current environment
    val stateKey    = memory.environment.toString
    val actionIndex = memory.environment.possibleActions.indexOf(memory.action)

    // 1.1. Critic
    val stateValue           = stateValueNetwork.predict(memory.environment.board.grid)

    val newCriticEligibilities = criticEligibilities + (stateKey -> 1.0)

    // 1.2 Actor
    val stateActionRewardList   = stateActionRewardMap.getOrElse(stateKey, memory.environment.possibleActions.map(action => ActionReward(action)))
    val newStateActionRewardMap = stateActionRewardMap + (stateKey -> stateActionRewardList)

    val actorEligibilityList    = actorEligibilities.getOrElse(stateKey, memory.environment.possibleActions.map(_ => 0.0))
    val newActorEligibilityList = actorEligibilityList.updated(actionIndex, 1.0)
    val newActorEligibilities   = actorEligibilities + (stateKey -> newActorEligibilityList)

    // 2. Next environment

    // 2.1 Critic
    val nextStateValue          = stateValueNetwork.predict(memory.environment.board.grid)
    val temporalDifferenceError = memory.nextEnvironment.reward + (criticDiscountFactor * nextStateValue) - stateValue

    // 3. New agent
    val newAgent = NetworkActorCriticAgent(
      initialEnvironment,
      stateActionRewardMap = newStateActionRewardMap,
      epsilonRate = epsilonRate,
      actorEligibilities = newActorEligibilities,
      criticEligibilities = newCriticEligibilities,
      stateValueNetwork = stateValueNetwork
    )

    step(memories, newAgent, temporalDifferenceError)
  }

  def step(memories: List[Memory], currentAgent: NetworkActorCriticAgent, temporalDifferenceError: Double, memoryIndex: Int = 0): ActorCriticAgent = {
    if (memoryIndex > memories.length - 1) {
      currentAgent
    } else {
      val memory   = memories(memoryIndex)
      val stateKey = memory.environment.toString

      // Critic
      val stateValue           = stateValueNetwork.predict(memory.environment.board.grid)
      val criticEligibility = currentAgent.criticEligibilities(stateKey)

      val newStateValue        = stateValue + (tableCriticLearningRate * temporalDifferenceError * criticEligibility)
      currentAgent.stateValueNetwork.fit(memory.environment.board.grid, newStateValue)

      val newCriticEligibility   = criticDiscountFactor * criticEligibilityDecayRate * criticEligibility
      val newCriticEligibilities = currentAgent.criticEligibilities + (stateKey -> newCriticEligibility)

      // Actor
      val actionIndex = memory.environment.possibleActions.indexOf(memory.action)

      val stateActionRewardList = currentAgent.stateActionRewardMap(stateKey)
      val stateActionReward     = stateActionRewardList(actionIndex)
      val reward                = stateActionReward.reward

      val actorEligibilityList = currentAgent.actorEligibilities(stateKey)
      val actorEligibility     = actorEligibilityList(actionIndex)

      val newReward                = reward + (actorLearningRate * temporalDifferenceError * actorEligibility)
      val newStateActionReward     = ActionReward(memory.action, newReward)
      val newStateActionRewardList = stateActionRewardList.updated(actionIndex, newStateActionReward)
      val newStateActionRewardMap  = currentAgent.stateActionRewardMap + (stateKey -> newStateActionRewardList)

      val newActorEligibility     = actorDiscountFactor * actorEligibilityDecayRate * actorEligibility
      val newActorEligibilityList = actorEligibilityList.updated(actionIndex, newActorEligibility)
      val newActorEligibilities   = currentAgent.actorEligibilities + (stateKey -> newActorEligibilityList)

      val newAgent = NetworkActorCriticAgent(
        initialEnvironment,
        stateActionRewardMap = newStateActionRewardMap,
        epsilonRate = epsilonRate,
        actorEligibilities = newActorEligibilities,
        criticEligibilities = newCriticEligibilities,
        stateValueNetwork = stateValueNetwork
      )

      step(memories, newAgent, temporalDifferenceError, memoryIndex = memoryIndex + 1)
    }
  }

  def updateEpsilonRate(): ActorCriticAgent = {
    val potentialNewEpsilonRate = epsilonRate * actorEpsilonDecayRate
    val newEpsilonRate          = if (potentialNewEpsilonRate >= actorEpsilonMinRate) potentialNewEpsilonRate else actorEpsilonMinRate
    NetworkActorCriticAgent(initialEnvironment, stateActionRewardMap = stateActionRewardMap, epsilonRate = newEpsilonRate, stateValueNetwork = stateValueNetwork)
  }

  def removeEpsilon(): ActorCriticAgent = {
    NetworkActorCriticAgent(initialEnvironment, stateActionRewardMap = stateActionRewardMap, epsilonRate = 0.0, stateValueNetwork = stateValueNetwork)
  }

  def resetEligibilities(): ActorCriticAgent = {
    NetworkActorCriticAgent(initialEnvironment, stateActionRewardMap = stateActionRewardMap, epsilonRate = epsilonRate, stateValueNetwork = stateValueNetwork)
  }

  override def toString: String = {
    s"StateActionRewardMap: ${stateActionRewardMap.size}, StateValueNetwork: ${stateValueNetwork.toString}, EpsilonRate: $epsilonRate"
  }
}