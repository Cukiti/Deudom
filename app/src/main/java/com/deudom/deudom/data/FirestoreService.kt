package com.deudom.deudom.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

import com.deudom.deudom.util.ScoreCalculator

object FirestoreService {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val usersCollection = db.collection("users")
    private val loansCollection = db.collection("loans")

    suspend fun uploadImage(path: String, data: ByteArray): String? {
        return try {
            val ref = storage.reference.child(path)
            ref.putBytes(data).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUser(user: User): Boolean {
        return try {
            usersCollection.document(user.phone).set(user).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUser(phone: String): User? {
        return try {
            val document = usersCollection.document(phone).get().await()
            if (document.exists()) {
                document.toObject(User::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateFcmToken(phone: String, token: String): Boolean {
        return try {
            usersCollection.document(phone).update("fcmToken", token).await()
            true
        } catch (e: Exception) {
            // If user doc doesn't exist yet, we can't update token, which is expected during signup flow
            false
        }
    }

    suspend fun updatePhotoUrl(phone: String, url: String): Boolean {
        return try {
            usersCollection.document(phone).update("photoUrl", url).await()
            
            // Propagate to friendships
            val sent = friendsCollection.whereEqualTo("senderPhone", phone).get().await()
            val received = friendsCollection.whereEqualTo("receiverPhone", phone).get().await()
            
            val batch = db.batch()
            sent.documents.forEach { doc -> batch.update(doc.reference, "senderPhotoUrl", url) }
            received.documents.forEach { doc -> batch.update(doc.reference, "receiverPhotoUrl", url) }
            
            if (!sent.isEmpty || !received.isEmpty) {
                batch.commit().await()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updatePin(phone: String, pin: String): Boolean {
        return try {
            usersCollection.document(phone).update("pin", pin).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateFinancialInfo(
        phone: String, 
        walletCash: Double, 
        balance: Double, 
        reserve: Double = 0.0, 
        baseCurrency: String = "RD$",
        lastWalletUpdateTimestamp: Long = 0,
        budgets: Map<String, Double>? = null
    ): Boolean {
        return try {
            val updates = mutableMapOf<String, Any>(
                "walletCash" to walletCash,
                "currentBalance" to balance,
                "emergencyReserve" to reserve,
                "baseCurrency" to baseCurrency
            )
            if (lastWalletUpdateTimestamp != 0L) updates["lastWalletUpdateTimestamp"] = lastWalletUpdateTimestamp
            if (budgets != null) updates["categoryBudgets"] = budgets
            
            usersCollection.document(phone).update(updates).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun findRegisteredUsers(phones: List<String>): List<User> {
        if (phones.isEmpty()) return emptyList()
        return try {
            val chunks = phones.chunked(30)
            val registeredUsers = mutableListOf<User>()
            for (chunk in chunks) {
                val snapshot = usersCollection.whereIn("phone", chunk).get().await()
                registeredUsers.addAll(snapshot.toObjects(User::class.java))
            }
            registeredUsers
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createLoan(loan: Loan): Boolean {
        return try {
            db.runTransaction { transaction ->
                val loanRef = loansCollection.document(loan.id)
                val snapshot = transaction.get(loanRef)
                
                var finalLoan = loan
                var wasAlreadyPaid = false
                var shouldAwardFullPayment = false
                var shouldAwardPartialPayment = false
                
                if (snapshot.exists()) {
                    val remoteLoan = snapshot.toObject(Loan::class.java)
                    if (remoteLoan != null) {
                        wasAlreadyPaid = remoteLoan.status == LoanStatus.PAID
                        
                        // Merge payments: keep all unique payments based on their date/amount
                        val mergedPayments = (remoteLoan.payments + loan.payments)
                            .distinctBy { "${it.date}_${it.amount}" }
                            .sortedBy { it.date }
                        
                        val finalPaidAmount = mergedPayments.sumOf { it.amount }.coerceAtLeast(remoteLoan.paidAmount).coerceAtLeast(loan.paidAmount)
                        
                        // Terminal status protection: if either is PAID, final is PAID.
                        // If either is OVERDUE (and not PAID), final is OVERDUE.
                        var finalStatus = when {
                            remoteLoan.status == LoanStatus.PAID || loan.status == LoanStatus.PAID -> LoanStatus.PAID
                            finalPaidAmount >= loan.amount - 0.01 -> LoanStatus.PAID
                            remoteLoan.status == LoanStatus.OVERDUE || loan.status == LoanStatus.OVERDUE -> LoanStatus.OVERDUE
                            remoteLoan.status == LoanStatus.PENDING || loan.status == LoanStatus.PENDING -> LoanStatus.PENDING
                            else -> loan.status
                        }
                        
                        // Fix: If final status is PAID, the paidAmount must be the total amount
                        val correctedPaidAmount = if (finalStatus == LoanStatus.PAID) loan.amount else finalPaidAmount
                        
                        finalLoan = loan.copy(
                            payments = mergedPayments,
                            paidAmount = correctedPaidAmount,
                            status = finalStatus,
                            // Ensure we don't lose the signature or other fields if the other user has them
                            signatureUrl = loan.signatureUrl ?: remoteLoan.signatureUrl,
                            borrowerPhone = if (loan.borrowerPhone.isBlank()) remoteLoan.borrowerPhone else loan.borrowerPhone,
                            borrowerName = if (loan.borrowerName.isBlank()) remoteLoan.borrowerName else loan.borrowerName,
                            lenderPhone = if (loan.lenderPhone.isBlank()) remoteLoan.lenderPhone else loan.lenderPhone,
                            lenderName = if (loan.lenderName.isBlank()) remoteLoan.lenderName else loan.lenderName,
                            description = if (loan.description.isBlank()) remoteLoan.description else loan.description,
                            category = if (loan.category == "Otros" && remoteLoan.category != "Otros") remoteLoan.category else loan.category,
                            currency = if (loan.currency.isBlank()) remoteLoan.currency else loan.currency,
                            dueDate = loan.dueDate ?: remoteLoan.dueDate,
                            installments = if (loan.installments.isEmpty()) remoteLoan.installments else loan.installments,
                            amount = if (loan.amount <= 0.0) remoteLoan.amount else loan.amount,
                            isSynced = true // Always true in Firestore
                        )
                        
                        if (!wasAlreadyPaid && finalLoan.status == LoanStatus.PAID && finalLoan.borrowerPhone.isNotBlank()) {
                            shouldAwardFullPayment = true
                        } else if (finalLoan.payments.size > remoteLoan.payments.size && finalLoan.borrowerPhone.isNotBlank()) {
                            shouldAwardPartialPayment = true
                        }
                    }
                } else {
                    if (finalLoan.status == LoanStatus.PAID && finalLoan.borrowerPhone.isNotBlank()) {
                        shouldAwardFullPayment = true
                    }
                }

                // READS: Fetch user doc before any writes
                val userRef = if ((shouldAwardFullPayment || shouldAwardPartialPayment) && finalLoan.borrowerPhone.isNotBlank()) {
                    usersCollection.document(finalLoan.borrowerPhone)
                } else null
                
                val userSnapshot = userRef?.let { transaction.get(it) }

                // WRITES: Now we can safely perform all writes
                transaction.set(loanRef, finalLoan)
                
                if (userSnapshot != null && userSnapshot.exists()) {
                    if (shouldAwardFullPayment) {
                        val currentScore = (userSnapshot.getLong("score") ?: 50L).toInt()
                        val currentCukitis = (userSnapshot.getLong("cukitis") ?: 0L).toInt()
                        val dueDate = finalLoan.dueDate
                        val lastPaymentDate = finalLoan.payments.maxOfOrNull { it.date } ?: System.currentTimeMillis()
                        
                        val isOnTime = dueDate == null || lastPaymentDate <= (dueDate + 86400000L)
                        val isEarly = dueDate != null && lastPaymentDate < (dueDate - 172800000L)
                        
                        val newScore = ScoreCalculator.calculateNewScore(currentScore, isOnTime, isEarly)
                        val fieldToIncrement = if (isOnTime) "goodPayCount" else "badPayCount"
                        
                        // Award 100 Cukitis for settling a debt
                        transaction.update(userRef, fieldToIncrement, FieldValue.increment(1))
                        transaction.update(userRef, "score", newScore)
                        transaction.update(userRef, "cukitis", currentCukitis + 100)
                    } else if (shouldAwardPartialPayment) {
                        val currentCukitis = (userSnapshot.getLong("cukitis") ?: 0L).toInt()
                        transaction.update(userRef, "cukitis", currentCukitis + 50)
                    }
                }
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun createMultipleLoans(loans: List<Loan>): Boolean {
        return try {
            val batch = db.batch()
            loans.forEach { loan ->
                val docRef = loansCollection.document(loan.id)
                batch.set(docRef, loan)
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addPayment(loanId: String, payment: Payment): Boolean {
        return try {
            val loanRef = loansCollection.document(loanId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(loanRef)
                val currentPaid = snapshot.getDouble("paidAmount") ?: 0.0
                val totalAmount = snapshot.getDouble("amount") ?: 0.0
                val borrowerPhone = snapshot.getString("borrowerPhone") ?: ""
                
                val newPaid = currentPaid + payment.amount
                val isNowPaid = newPaid >= (totalAmount - 0.01) // Handle potential float precision issues
                val newStatus = if (isNowPaid) LoanStatus.PAID else LoanStatus.PENDING
                
                // READS: Fetch user doc before any writes
                val userRef = if (borrowerPhone.isNotBlank()) usersCollection.document(borrowerPhone) else null
                val userSnapshot = userRef?.let { transaction.get(it) }

                // WRITES:
                transaction.update(loanRef, "paidAmount", newPaid)
                transaction.update(loanRef, "status", newStatus)
                transaction.update(loanRef, "payments", FieldValue.arrayUnion(payment))
                
                // --- SCORE & COMPLIANCE UPDATE ---
                if (userSnapshot != null && userSnapshot.exists()) {
                    val currentScore = (userSnapshot.getLong("score") ?: 50L).toInt()
                    val dueDate = snapshot.getLong("dueDate")
                    val paymentDate = payment.date
                    
                    // Rule: Paying on time or early adds points
                    val isOnTime = dueDate == null || paymentDate <= (dueDate + 86400000L) // 1 day grace period
                    val isEarly = dueDate != null && paymentDate < (dueDate - 172800000L) // 2 days before
                    
                    // Update stats
                    if (isNowPaid) {
                        val newScore = ScoreCalculator.calculateNewScore(currentScore, isOnTime, isEarly)
                        val fieldToIncrement = if (isOnTime) "goodPayCount" else "badPayCount"
                        transaction.update(userRef, fieldToIncrement, FieldValue.increment(1))
                        transaction.update(userRef, "score", newScore)
                    }
                }
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getLoansAsLender(phone: String): List<Loan> {
        return try {
            val snapshot = loansCollection.whereEqualTo("lenderPhone", phone).get().await()
            snapshot.toObjects(Loan::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getLoansAsBorrower(phone: String): List<Loan> {
        return try {
            val snapshot = loansCollection.whereEqualTo("borrowerPhone", phone).get().await()
            snapshot.toObjects(Loan::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateLoanStatus(loan: Loan, status: LoanStatus): Boolean {
        return try {
            val loanRef = loansCollection.document(loan.id)
            db.runTransaction { transaction ->
                transaction.update(loanRef, "status", status)
                
                if (status == LoanStatus.PAID) {
                    transaction.update(loanRef, "paidAmount", loan.amount)
                    
                    val now = System.currentTimeMillis()
                    val isOnTime = loan.dueDate == null || now <= (loan.dueDate + 86400000L)
                    val userRef = usersCollection.document(loan.borrowerPhone)
                    
                    val userSnapshot = transaction.get(userRef)
                    if (userSnapshot.exists()) {
                        val currentScore = (userSnapshot.getLong("score") ?: 50L).toInt()
                        val newScore = ScoreCalculator.calculateNewScore(currentScore, isOnTime)
                        
                        val fieldToIncrement = if (isOnTime) "goodPayCount" else "badPayCount"
                        transaction.update(userRef, fieldToIncrement, FieldValue.increment(1))
                        transaction.update(userRef, "score", newScore)
                    }
                }
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun simplifyDebts(loanA: Loan, loanB: Loan): Boolean {
        return try {
            db.runTransaction { transaction ->
                val refA = loansCollection.document(loanA.id)
                val refB = loansCollection.document(loanB.id)
                
                val netA = loanA.amount - loanA.paidAmount
                val netB = loanB.amount - loanB.paidAmount
                
                // READS: Fetch user docs before any writes
                val userRefA = if (loanA.borrowerPhone.isNotBlank()) usersCollection.document(loanA.borrowerPhone) else null
                val userSnapshotA = userRefA?.let { transaction.get(it) }
                
                val userRefB = if (loanB.borrowerPhone.isNotBlank()) usersCollection.document(loanB.borrowerPhone) else null
                val userSnapshotB = userRefB?.let { transaction.get(it) }

                // WRITES
                if (netA >= netB) {
                    // Loan B is fully covered by A
                    transaction.update(refB, "paidAmount", loanB.amount)
                    transaction.update(refB, "status", LoanStatus.PAID)
                    transaction.update(refA, "paidAmount", loanA.paidAmount + netB)
                    
                    // Update score for borrower of Loan B
                    if (userSnapshotB != null && userSnapshotB.exists() && userRefB != null) {
                        val currentScore = (userSnapshotB.getLong("score") ?: 50L).toInt()
                        val now = System.currentTimeMillis()
                        val isOnTime = loanB.dueDate == null || now <= (loanB.dueDate + 86400000L)
                        val isEarly = loanB.dueDate != null && now < (loanB.dueDate - 172800000L)
                        
                        val newScore = ScoreCalculator.calculateNewScore(currentScore, isOnTime, isEarly)
                        val fieldToIncrement = if (isOnTime) "goodPayCount" else "badPayCount"
                        transaction.update(userRefB, fieldToIncrement, FieldValue.increment(1))
                        transaction.update(userRefB, "score", newScore)
                    }
                    
                    if (netA - netB < 0.01) {
                         // A is also exactly covered
                         transaction.update(refA, "status", LoanStatus.PAID)
                         if (userSnapshotA != null && userSnapshotA.exists() && userRefA != null) {
                            val currentScore = (userSnapshotA.getLong("score") ?: 50L).toInt()
                            val now = System.currentTimeMillis()
                            val isOnTime = loanA.dueDate == null || now <= (loanA.dueDate + 86400000L)
                            val isEarly = loanA.dueDate != null && now < (loanA.dueDate - 172800000L)
                            
                            val newScore = ScoreCalculator.calculateNewScore(currentScore, isOnTime, isEarly)
                            val fieldToIncrement = if (isOnTime) "goodPayCount" else "badPayCount"
                            transaction.update(userRefA, fieldToIncrement, FieldValue.increment(1))
                            transaction.update(userRefA, "score", newScore)
                        }
                    }
                } else {
                    // Loan A is fully covered by B
                    transaction.update(refA, "paidAmount", loanA.amount)
                    transaction.update(refA, "status", LoanStatus.PAID)
                    transaction.update(refB, "paidAmount", loanB.paidAmount + netA)
                    
                    // Update score for borrower of Loan A
                    if (userSnapshotA != null && userSnapshotA.exists() && userRefA != null) {
                        val currentScore = (userSnapshotA.getLong("score") ?: 50L).toInt()
                        val now = System.currentTimeMillis()
                        val isOnTime = loanA.dueDate == null || now <= (loanA.dueDate + 86400000L)
                        val isEarly = loanA.dueDate != null && now < (loanA.dueDate - 172800000L)
                        
                        val newScore = ScoreCalculator.calculateNewScore(currentScore, isOnTime, isEarly)
                        val fieldToIncrement = if (isOnTime) "goodPayCount" else "badPayCount"
                        transaction.update(userRefA, fieldToIncrement, FieldValue.increment(1))
                        transaction.update(userRefA, "score", newScore)
                    }
                }
            }.await()
            true
        } catch (e: Exception) { false }
    }

    fun listenToLoansAsLender(phone: String, onUpdate: (List<Loan>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return loansCollection.whereEqualTo("lenderPhone", phone).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(Loan::class.java))
        }
    }

    fun listenToLoansAsBorrower(phone: String, onUpdate: (List<Loan>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return loansCollection.whereEqualTo("borrowerPhone", phone).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(Loan::class.java))
        }
    }

    private val friendsCollection = db.collection("friendships")
    private val recurringCollection = db.collection("recurring_loans")
    private val groupsCollection = db.collection("groups")
    private val personalExpensesCollection = db.collection("personal_expenses")
    private val savingGoalsCollection = db.collection("saving_goals")

    suspend fun createSavingGoal(goal: SavingGoal): Boolean {
        return try {
            savingGoalsCollection.document(goal.id).set(goal).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getSavingGoals(phone: String): List<SavingGoal> {
        return try {
            savingGoalsCollection.whereEqualTo("userPhone", phone).get().await().toObjects(SavingGoal::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun deleteSavingGoal(id: String): Boolean {
        return try {
            savingGoalsCollection.document(id).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun addPersonalExpense(expense: PersonalExpense): Boolean {
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(expense.userPhone)
                val user = transaction.get(userRef).toObject(User::class.java)
                if (user != null) {
                    val newWalletCash = user.walletCash - expense.amount
                    transaction.update(userRef, "walletCash", newWalletCash)
                    transaction.set(personalExpensesCollection.document(expense.id), expense)
                }
            }.await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getPersonalExpenses(phone: String): List<PersonalExpense> {
        return try {
            personalExpensesCollection.whereEqualTo("userPhone", phone).get().await().toObjects(PersonalExpense::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun createGroup(group: Group): Boolean {
        return try {
            groupsCollection.document(group.id).set(group).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getGroups(phone: String): List<Group> {
        return try {
            // Firestore doesn't support array-contains-any with multiple objects easily, 
            // but we can query by a simple list of member phones if we store them separately 
            // or just fetch and filter for now (demo scale).
            val snapshot = groupsCollection.get().await()
            snapshot.toObjects(Group::class.java).filter { g -> g.members.any { it.phone == phone } }
        } catch (e: Exception) { emptyList() }
    }

    fun listenToGroups(phone: String, onUpdate: (List<Group>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return groupsCollection.addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val allGroups = snapshot.toObjects(Group::class.java)
                onUpdate(allGroups.filter { g -> g.members.any { it.phone == phone } })
            }
        }
    }

    suspend fun addGroupExpense(groupId: String, expense: GroupExpense): Boolean {
        return try {
            groupsCollection.document(groupId).collection("expenses").document(expense.id).set(expense).await()
            true
        } catch (e: Exception) { false }
    }

    fun listenToGroupExpenses(groupId: String, onUpdate: (List<GroupExpense>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return groupsCollection.document(groupId).collection("expenses").orderBy("date").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(GroupExpense::class.java))
        }
    }

    fun listenToRecurringLoans(phone: String, onUpdate: (List<RecurringLoan>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return recurringCollection.whereEqualTo("creatorPhone", phone).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(RecurringLoan::class.java))
        }
    }

    suspend fun createRecurringLoan(recurring: RecurringLoan): Boolean {
        return try {
            recurringCollection.document(recurring.id).set(recurring).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getRecurringLoans(phone: String): List<RecurringLoan> {
        return try {
            val snapshot = recurringCollection.whereEqualTo("creatorPhone", phone).get().await()
            snapshot.toObjects(RecurringLoan::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateRecurringLoan(recurring: RecurringLoan): Boolean {
        return try {
            recurringCollection.document(recurring.id).set(recurring).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteRecurringLoan(id: String): Boolean {
        return try {
            recurringCollection.document(id).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun sendFriendRequest(request: FriendRequest): Boolean {
        return try {
            // Directly set the request. Duplication check is handled by UI/Logic before calling this.
            friendsCollection.document(request.id).set(request).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun respondFriendRequest(requestId: String, status: FriendshipStatus): Boolean {
        return try {
            friendsCollection.document(requestId).update("status", status).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun deleteFriendship(requestId: String): Boolean {
        return try {
            friendsCollection.document(requestId).delete().await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun getFriends(phone: String): List<FriendRequest> {
        return try {
            val sent = friendsCollection.whereEqualTo("senderPhone", phone).whereEqualTo("status", FriendshipStatus.ACCEPTED).get().await().toObjects(FriendRequest::class.java)
            val received = friendsCollection.whereEqualTo("receiverPhone", phone).whereEqualTo("status", FriendshipStatus.ACCEPTED).get().await().toObjects(FriendRequest::class.java)
            sent + received
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getPendingRequests(phone: String): List<FriendRequest> {
        return try {
            val snapshot = friendsCollection.whereEqualTo("receiverPhone", phone).whereEqualTo("status", FriendshipStatus.PENDING).get().await()
            snapshot.toObjects(FriendRequest::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getSentPendingRequests(phone: String): List<FriendRequest> {
        return try {
            val snapshot = friendsCollection.whereEqualTo("senderPhone", phone).whereEqualTo("status", FriendshipStatus.PENDING).get().await()
            snapshot.toObjects(FriendRequest::class.java)
        } catch (e: Exception) { emptyList() }
    }

    fun listenToSentPendingRequests(phone: String, onUpdate: (List<FriendRequest>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return friendsCollection.whereEqualTo("senderPhone", phone).whereEqualTo("status", FriendshipStatus.PENDING).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(FriendRequest::class.java))
        }
    }

    fun listenToFriendsAsSender(phone: String, onUpdate: (List<FriendRequest>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return friendsCollection.whereEqualTo("senderPhone", phone).whereEqualTo("status", FriendshipStatus.ACCEPTED).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(FriendRequest::class.java))
        }
    }

    fun listenToFriendsAsReceiver(phone: String, onUpdate: (List<FriendRequest>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return friendsCollection.whereEqualTo("receiverPhone", phone).whereEqualTo("status", FriendshipStatus.ACCEPTED).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(FriendRequest::class.java))
        }
    }

    fun listenToPendingRequests(phone: String, onUpdate: (List<FriendRequest>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return friendsCollection.whereEqualTo("receiverPhone", phone).whereEqualTo("status", FriendshipStatus.PENDING).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(FriendRequest::class.java))
        }
    }

    fun listenToUser(phone: String, onUpdate: (User?) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return usersCollection.document(phone).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                onUpdate(snapshot.toObject(User::class.java))
            } else {
                onUpdate(null)
            }
        }
    }

    fun listenToPersonalExpenses(phone: String, onUpdate: (List<PersonalExpense>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return personalExpensesCollection.whereEqualTo("userPhone", phone).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(PersonalExpense::class.java))
        }
    }

    fun listenToSavingGoals(phone: String, onUpdate: (List<SavingGoal>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return savingGoalsCollection.whereEqualTo("userPhone", phone).addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(SavingGoal::class.java))
        }
    }

    // Chat Logic
    suspend fun sendChatMessage(loanId: String, message: ChatMessage): Boolean {
        return try {
            loansCollection.document(loanId).collection("chat").document(message.id).set(message).await()
            true
        } catch (e: Exception) { false }
    }

    fun listenToChat(loanId: String, onUpdate: (List<ChatMessage>) -> Unit): com.google.firebase.firestore.ListenerRegistration {
        return loansCollection.document(loanId).collection("chat").orderBy("timestamp").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) onUpdate(snapshot.toObjects(ChatMessage::class.java))
        }
    }

    suspend fun purchaseReward(phone: String, cost: Int, rewardId: String): Boolean {
        return try {
            db.runTransaction { transaction ->
                val userRef = usersCollection.document(phone)
                val snapshot = transaction.get(userRef)
                val currentCukitis = (snapshot.getLong("cukitis") ?: 0).toInt()
                val unlocked = snapshot.get("unlockedThemes") as? List<String> ?: emptyList()

                if (currentCukitis >= cost && !unlocked.contains(rewardId)) {
                    transaction.update(userRef, "cukitis", currentCukitis - cost)
                    transaction.update(userRef, "unlockedThemes", FieldValue.arrayUnion(rewardId))
                } else {
                    throw Exception("Insufficient funds or already owned")
                }
            }.await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteUserAccount(phone: String): Boolean {
        return try {
            // Delete user document
            usersCollection.document(phone).delete().await()
            // Auth deletion handled by client SDK or Cloud Functions typically
            // Here we just clear the DB record
            true
        } catch (e: Exception) {
            false
        }
    }
}
