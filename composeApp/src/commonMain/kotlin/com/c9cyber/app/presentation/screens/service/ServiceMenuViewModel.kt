package com.c9cyber.app.presentation.screens.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.c9cyber.app.domain.model.CartItem
import com.c9cyber.app.domain.model.ServiceItem
import com.c9cyber.app.domain.smartcard.PinVerifyResult
import com.c9cyber.app.domain.smartcard.SmartCardManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class ServiceMenuUiState(
    val serviceItems: List<ServiceItem> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showPinDialog: Boolean = false,
    val pinError: String? = null,
    val isPaymentSuccess: Boolean = false
)

class ServiceMenuViewModel(
    private val smartCardManager: SmartCardManager
) {
    var uiState by mutableStateOf(ServiceMenuUiState())
        private set

    private val viewModelScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        loadServiceItems()
    }

    private fun loadServiceItems() {
        // Tạo dữ liệu mẫu đa dạng gồm đồ ăn và đồ uống
        val items = listOf(
            ServiceItem("1", "Sting Dâu", 12000, "food_placeholder"),
            ServiceItem("2", "Coca Cola", 10000, "food_placeholder"),
            ServiceItem("3", "Red Bull", 15000, "food_placeholder"),
            ServiceItem("4", "Mì Tôm Trứng", 25000, "food_placeholder"),
            ServiceItem("5", "Mì Cay 7 Cấp", 45000, "food_placeholder"),
            ServiceItem("6", "Cơm Rang Dưa Bò", 40000, "food_placeholder"),
            ServiceItem("7", "Bánh Mì Pate", 20000, "food_placeholder"),
            ServiceItem("8", "Trà Đào Cam Sả", 30000, "food_placeholder"),
            ServiceItem("9", "Cafe Sữa Đá", 25000, "food_placeholder")
        )
        uiState = uiState.copy(serviceItems = items)
    }

    // Thêm vào giỏ hàng hoặc tăng số lượng
    fun addToCart(item: ServiceItem) {
        val currentCart = uiState.cartItems.toMutableList()
        val existingItemIndex = currentCart.indexOfFirst { it.serviceItem.id == item.id }

        if (existingItemIndex != -1) {
            val existingItem = currentCart[existingItemIndex]
            currentCart[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity + 1)
        } else {
            currentCart.add(CartItem(item, 1))
        }
        uiState = uiState.copy(cartItems = currentCart)
    }

    // Giảm số lượng, nếu còn 1 thì xóa khỏi giỏ (hoặc giữ nguyên tùy logic, ở đây là giảm tới 1 rồi cần nút xóa riêng để xóa hẳn, hoặc giảm về 0 thì xóa)
    // Logic hiện tại: Giảm số lượng, nếu về 0 thì xóa
    fun removeFromCart(item: ServiceItem) {
        val currentCart = uiState.cartItems.toMutableList()
        val existingItemIndex = currentCart.indexOfFirst { it.serviceItem.id == item.id }

        if (existingItemIndex != -1) {
            val existingItem = currentCart[existingItemIndex]
            if (existingItem.quantity > 1) {
                currentCart[existingItemIndex] = existingItem.copy(quantity = existingItem.quantity - 1)
            } else {
                currentCart.removeAt(existingItemIndex)
            }
            uiState = uiState.copy(cartItems = currentCart)
        }
    }
    
    // Xóa hẳn món đó khỏi giỏ hàng bất kể số lượng
    fun removeEntireItem(item: ServiceItem) {
        val currentCart = uiState.cartItems.toMutableList()
        currentCart.removeAll { it.serviceItem.id == item.id }
        uiState = uiState.copy(cartItems = currentCart)
    }

    fun showPinDialog() {
        if (uiState.cartItems.isNotEmpty()) {
            uiState = uiState.copy(showPinDialog = true, pinError = null)
        }
    }

    fun hidePinDialog() {
        uiState = uiState.copy(showPinDialog = false, pinError = null)
    }

    // Xử lý thanh toán
    fun processPayment(pin: String) {
        uiState = uiState.copy(isLoading = true, pinError = null)
        viewModelScope.launch {
            // 1. Xác thực PIN thẻ
            val verifyResult = smartCardManager.verifyPin(pin)
            when (verifyResult) {
                is PinVerifyResult.Success -> {
                    // 2. Nếu PIN đúng, thực hiện trừ tiền (Logic trừ tiền sẽ cần gọi xuống thẻ hoặc API)
                    // Hiện tại giả lập thanh toán thành công
                    
                    // TODO: Gọi hàm trừ tiền trong thẻ (ví dụ: smartCardManager.deductBalance(total))
                    
                    uiState = uiState.copy(
                        isLoading = false,
                        showPinDialog = false,
                        cartItems = emptyList(), // Xóa giỏ hàng sau khi thanh toán
                        isPaymentSuccess = true
                    )
                }
                is PinVerifyResult.CardLocked -> {
                    uiState = uiState.copy(isLoading = false, pinError = "Thẻ đã bị khóa do nhập sai quá nhiều lần")
                }
                is PinVerifyResult.WrongPin -> {
                    uiState = uiState.copy(isLoading = false, pinError = "Sai PIN. Còn lại ${verifyResult.remainingTries} lần thử")
                }
                is PinVerifyResult.Error -> {
                    uiState = uiState.copy(isLoading = false, pinError = verifyResult.message)
                }
            }
        }
    }
    
    fun resetPaymentSuccess() {
        uiState = uiState.copy(isPaymentSuccess = false)
    }

    // Tính tổng tiền
    fun calculateTotal(): Long {
        return uiState.cartItems.sumOf { it.serviceItem.price * it.quantity }
    }
}
