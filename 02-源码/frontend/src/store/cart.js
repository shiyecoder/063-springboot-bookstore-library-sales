import { reactive, watch } from 'vue'

/**
 * 购物车：单机演示场景不做服务端购物车表，落在 localStorage，
 * 提交订单时一次性把明细发给 /api/order（后端负责库存与金额复核）。
 */
const KEY = 'book_cart_v1'

function load() {
  try {
    const raw = JSON.parse(localStorage.getItem(KEY) || '[]')
    return Array.isArray(raw) ? raw : []
  } catch (e) {
    return []
  }
}

export const cart = reactive({ items: load() })

watch(
  () => cart.items,
  (v) => localStorage.setItem(KEY, JSON.stringify(v)),
  { deep: true }
)

export const cartCount = () => cart.items.reduce((s, i) => s + i.qty, 0)
export const cartAmount = () => cart.items.reduce((s, i) => s + i.qty * Number(i.salePrice || 0), 0)

cart.add = (book, qty = 1) => {
  const hit = cart.items.find((i) => i.id === book.id)
  const max = Math.max(1, Number(book.stockQty || 1))
  if (hit) hit.qty = Math.min(max, hit.qty + qty)
  else
    cart.items.push({
      id: book.id,
      bookName: book.bookName,
      author: book.author,
      coverImage: book.coverImage,
      salePrice: Number(book.salePrice || 0),
      stockQty: Number(book.stockQty || 0),
      qty: Math.min(max, qty)
    })
}

cart.setQty = (id, qty) => {
  const hit = cart.items.find((i) => i.id === id)
  if (hit) hit.qty = Math.max(1, Math.min(Number(hit.stockQty || 99), Number(qty) || 1))
}

cart.remove = (id) => {
  cart.items = cart.items.filter((i) => i.id !== id)
}

cart.clear = () => {
  cart.items = []
}

cart.count = 0
watch(
  () => cart.items,
  () => {
    cart.count = cartCount()
  },
  { deep: true, immediate: true }
)
