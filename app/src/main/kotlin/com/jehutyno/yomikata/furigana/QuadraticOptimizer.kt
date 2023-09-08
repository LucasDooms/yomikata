/*
 * FuriganaView widget
 * Copyright (C) 2013 sh0 <sh0@yutani.ee>
 * Licensed under Creative Commons BY-SA 3.0
 */
// Package
package com.jehutyno.yomikata.furigana

import kotlin.math.min

// Constraint optimizer class
class QuadraticOptimizer(// Variables
    var m_a: Array<FloatArray>, var m_b: FloatArray
) {
    // Constructor
    init {
        // Variables
        assert(m_b.size == m_a.size)
    }

    // Calculate
    fun calculate(x: FloatArray) {
        // Check if calculation needed
        if (phi(1.0f, x) == 0.0f) return

        // Calculate
        var sigma = 1.0f
        for (k in 0 until m_penalty_runs) {
            newton_solve(x, sigma)
            sigma *= m_sigma_mul
        }
    }

    private fun newton_solve(x: FloatArray, sigma: Float) {
        for (i in 0 until m_newton_runs) newton_iteration(x, sigma)
    }

    private fun newton_iteration(x: FloatArray, sigma: Float) {
        // Calculate gradient
        val d = FloatArray(x.size)
        for (i in d.indices) d[i] = phi_d1(i, sigma, x)

        // Calculate Hessian matrix (symmetric)
        val h = Array(x.size) { FloatArray(x.size) }
        for (i in h.indices) for (j in i until h[0].size) h[i][j] = phi_d2(i, j, sigma, x)
        for (i in h.indices) for (j in 0 until i) h[i][j] = h[j][i]

        /*
        // Debug
        //Log.w("newton_solve", "<========================================>");
        Log.w("newton_solve", String.format("phi = %f", phi(sigma, x)));
        
        // Debug
        String str = "";
        for (int i = 0; i < d.length; i++)
            str += String.format("%.3f ", d[i]);
        Log.w("newton_solve", "d = [ " + str + "]");
        
        // Debug
        for (int i = 0; i < h.length; i++) {
            str = "";
            for (int j = 0; j < h[0].length; j++)
                str += String.format("%.3f ", h[i][j]);
            Log.w("newton_solve", String.format("h[%02d] = [ %s]", i, str));
        }
        */

        // Linear system solver
        val p = gs_solver(h, d)

        // Iteration
        for (i in x.indices) x[i] = x[i] - m_wolfe_gamma * p[i]

        /*
        // Debug
        str = "";
        for (int i = 0; i < x.length; i++)
            str += String.format("%.3f ", x[i]);
        Log.w("newton_solve", "x = [ " + str + "]");
        */
    }

    // Gauss-Seidel solver
    private fun gs_solver(a: Array<FloatArray>, b: FloatArray): FloatArray {
        // Initial guess
        val p = FloatArray(b.size)
        for (i in p.indices) p[i] = 1.0f
        for (z in 0 until m_gs_runs) {
            for (i in p.indices) {
                var s = 0.0f
                for (j in p.indices) {
                    if (i != j) s += a[i][j] * p[j]
                }
                p[i] = (b[i] - s) / a[i][i]
            }
        }

        // Result
        return p
    }

    // Math
    private fun dot(a: FloatArray, b: FloatArray): Float {
        assert(a.size == b.size)
        var r = 0.0f
        for (i in a.indices) r += a[i] * b[i]
        return r
    }

    // Cost function f(x)
    private fun f(x: FloatArray): Float {
        return dot(x, x)
    }

    // Cost function phi(x)
    private fun phi(sigma: Float, x: FloatArray): Float {
        var r = 0.0f
        for (i in x.indices) r += min(0.0, (dot(m_a[i], x) - m_b[i]).toDouble()).pow(2.0)
        return f(x) + sigma * r
    }

    private fun phi_d1(n: Int, sigma: Float, x: FloatArray): Float {
        var r = 0.0f
        for (i in m_a.indices) {
            val c = dot(m_a[i], x) - m_b[i]
            if (c < 0) r += 2.0f * m_a[i][n] * c
        }
        return 2.0f * x[n] + sigma * r
    }

    private fun phi_d2(n: Int, m: Int, sigma: Float, x: FloatArray): Float {
        var r = 0.0f
        for (i in m_a.indices) {
            val c = dot(m_a[i], x) - m_b[i]
            if (c < 0) r += 2.0f * m_a[i][n] * m_a[i][m]
        }
        return (if (n == m) 2.0f else 0.0f) + sigma * r
    }

    companion object {
        // Constants
        const val m_wolfe_gamma = 0.1f
        const val m_sigma_mul = 10.0f
        const val m_penalty_runs = 5
        const val m_newton_runs = 20
        const val m_gs_runs = 20
    }
}
