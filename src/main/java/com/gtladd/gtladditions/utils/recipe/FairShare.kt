package com.gtladd.gtladditions.utils.recipe

object FairShare {
    /** [shares] holds the wants on entry and the shares on return; [allFit] means the budgeted wants already fit. */
    fun allocate(
        shares: LongArray,
        budgeted: BooleanArray,
        budget: Long,
        size: Int,
        budgetedCount: Int,
        allFit: Boolean
    ): LongArray {
        if (allFit) return shares

        if (budget <= budgetedCount) {
            var remaining = budget

            for (i in 0 until size) {
                if (!budgeted[i]) continue
                shares[i] = if (remaining > 0) {
                    remaining--
                    1
                } else {
                    0
                }
            }

            return shares
        }

        return distribute(shares, budgeted, budget, size, budgetedCount)
    }

    private fun distribute(
        shares: LongArray,
        budgeted: BooleanArray,
        budget: Long,
        size: Int,
        count: Int
    ): LongArray {
        var remaining = budget
        var active = count
        var paidThrough = 0L
        var level: Long

        while (true) {
            level = remaining / active
            var capped = 0

            for (i in 0 until size) {
                val want = shares[i]
                if (budgeted[i] && want > paidThrough && want <= level) {
                    remaining -= want
                    capped++
                }
            }

            if (capped == 0) break

            active -= capped
            paidThrough = level
        }

        var extra = remaining % active
        for (i in 0 until size) {
            if (budgeted[i] && shares[i] > level) {
                shares[i] = level
                if (extra > 0) {
                    shares[i]++
                    extra--
                }
            }
        }

        return shares
    }
}