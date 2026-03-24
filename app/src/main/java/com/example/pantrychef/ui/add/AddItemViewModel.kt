// ViewModel for AddItemSheet: merges instant local suggestions with OpenFoodFacts results and manages loading state.
package com.example.pantrychef.ui.add

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantrychef.core.OpenFoodFactsRepository
import com.example.pantrychef.core.OffSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val foodRepo: OpenFoodFactsRepository
) : ViewModel() {

    private val _suggestions = MutableStateFlow<List<OffSuggestion>>(emptyList())
    val suggestions: StateFlow<List<OffSuggestion>> = _suggestions

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private var searchJob: Job? = null

    private val commonItems = listOf(
        "Apples","Bananas","Oranges","Lemons","Limes","Grapes","Strawberries","Blueberries",
        "Raspberries","Cherries","Peaches","Pears","Plums","Mangoes","Pineapple","Kiwi",
        "Melon","Watermelon","Avocados","Coconut","Dates","Figs",
        "Tomatoes","Potatoes","Sweet potatoes","Onions","Garlic","Carrots","Celery","Broccoli",
        "Cauliflower","Spinach","Kale","Lettuce","Zucchini","Cucumber","Eggplant","Bell pepper",
        "Mushrooms","Leeks","Asparagus","Peas","Corn","Cabbage","Radish","Beetroot",
        "Beef","Pork","Chicken","Turkey","Duck","Lamb","Sausage","Ham","Bacon","Fish","Tuna",
        "Salmon","Shrimp","Eggs","Tofu","Tempeh","Beans","Chickpeas","Lentils",
        "Bread","Baguette","Tortillas","Pasta","Rice","Brown rice","Quinoa","Oats","Couscous",
        "Flour","Cornmeal","Cereal","Granola","Crackers","Biscuits","Muffins","Pancake mix",
        "Milk","Whole milk","Skim milk","Yogurt","Greek yogurt","Butter","Cream","Cheese",
        "Cottage cheese","Mozzarella","Parmesan","Cream cheese","Ice cream",
        "Sugar","Brown sugar","Powdered sugar","Salt","Pepper","Olive oil","Vegetable oil",
        "Sesame oil","Vinegar","Balsamic vinegar","Soy sauce","Ketchup","Mayonnaise","Mustard",
        "Hot sauce","Honey","Maple syrup","Peanut butter","Jam","Nutella","Cocoa powder",
        "Baking powder","Baking soda","Yeast","Vanilla extract",
        "Canned tomatoes","Tomato paste","Tomato sauce","Canned beans","Canned corn",
        "Canned tuna","Canned salmon","Canned soup","Pickles","Olives","Capers","Sauerkraut",
        "Water","Sparkling water","Juice","Apple juice","Orange juice","Coffee","Tea",
        "Green tea","Black tea","Herbal tea","Soda","Coke","Cola","Lemonade",
        "Beer","Wine","Red wine","White wine",
        "Frozen vegetables","Frozen berries","Frozen pizza","Frozen peas","Frozen corn",
        "Frozen fries","Ice cream",
        "Chips","Popcorn","Pretzels","Chocolate","Dark chocolate","Candy","Cookies","Nuts",
        "Almonds","Walnuts","Cashews","Peanuts","Trail mix","Protein bar",
        "Basil","Oregano","Thyme","Rosemary","Parsley","Cilantro","Paprika","Chili powder",
        "Cinnamon","Nutmeg","Cloves","Curry powder","Ginger","Turmeric","Garlic powder",
        "Onion powder","Bay leaves","Cumin","Cardamom","Vanilla","Peppercorns",
        "Kartoffeln","Zwiebeln","Knoblauch","Karotten","Paprika","Gurken","Spinat","Brot",
        "Reis","Nudeln","Käse","Milch","Eier","Butter","Wurst","Schinken","Lachs","Apfel",
        "Banane","Tomate","Zucker","Mehl","Essig","Öl"
    )

    fun setQuery(q: String) {
        _query.value = q
        searchJob?.cancel()

        val trimmed = q.trim()

        val local = buildLocalSuggestions(trimmed)
        _suggestions.value = local

        if (trimmed.length < 2) {
            Log.d("OFF_VM", "skip OFF (short query): '$trimmed'")
            _loading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(250)
            _loading.value = true
            Log.d("OFF_VM", "OFF search start: '$trimmed'")
            try {
                val off = withTimeout(10_000) { foodRepo.searchFoods(trimmed) }
                val merged = (local + off).distinctBy { it.label.lowercase() }.take(10)
                Log.d(
                    "OFF_VM",
                    "OFF search end: '$trimmed' -> ${off.size} off, ${merged.size} merged"
                )
                _suggestions.value = merged
            } catch (t: TimeoutCancellationException) {
                Log.w("OFF_VM", "OFF search TIMEOUT for '$trimmed'")
                _suggestions.value = local
            } catch (e: Exception) {
                if (e.message?.contains("Job was cancelled", true) == true ||
                    e.message?.contains("StandaloneCoroutine was cancelled", true) == true
                ) {
                    Log.d("OFF_VM", "OFF search cancelled for '$trimmed'")
                } else {
                    Log.e("OFF_VM", "OFF search error for '$trimmed': ${e.message}")
                }
                _suggestions.value = local
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearSuggestions() {
        searchJob?.cancel()
        _suggestions.value = emptyList()
        _loading.value = false
    }

    private fun buildLocalSuggestions(q: String): List<OffSuggestion> {
        if (q.isBlank()) return emptyList()
        val qLower = q.lowercase()
        return commonItems
            .asSequence()
            .filter { it.lowercase().contains(qLower) }
            .sortedWith(
                compareBy<String> { !it.lowercase().startsWith(qLower) }
                    .thenBy { it.lowercase() }
            )
            .map { name ->
                OffSuggestion(
                    code = "",
                    label = name,
                    name = name,
                    brand = "",
                    quantity = 1.0,
                    unit = "pcs"
                )
            }
            .take(10)
            .toList()
    }
}
