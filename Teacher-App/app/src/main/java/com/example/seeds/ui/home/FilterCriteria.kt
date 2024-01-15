package com.example.seeds.ui.home

data class FilterCriteria(
    val languages: Set<String> = emptySet(),
    val experiences: Set<String> = emptySet()
    // Add more filter types if needed
)
