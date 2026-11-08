import { Component, computed, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface EmojiItem {
  char: string;
  name: string;
  keywords: string;
}

export interface EmojiSection {
  id: string;
  name: string;
  icon: string;
  emojis: EmojiItem[];
}

export const EMOJI_SECTIONS: EmojiSection[] = [
  {
    id: 'fast-food',
    name: 'Comida Rápida',
    icon: '🍔',
    emojis: [
      { char: '🍔', name: 'Hamburguesa', keywords: 'burger hamburguesa fast food' },
      { char: '🍟', name: 'Papas Fritas', keywords: 'papas fritas fries patatas' },
      { char: '🍕', name: 'Pizza', keywords: 'pizza rebanada slice' },
      { char: '🌭', name: 'Hot Dog', keywords: 'hot dog pancho perro caliente' },
      { char: '🥪', name: 'Sandwich', keywords: 'sandwich emparedado bocadillo' },
      { char: '🌮', name: 'Taco', keywords: 'taco mexicano' },
      { char: '🌯', name: 'Burrito', keywords: 'burrito roll wrap' },
    ],
  },
  {
    id: 'meats',
    name: 'Carnes y Platos',
    icon: '🥩',
    emojis: [
      { char: '🥩', name: 'Carne', keywords: 'carne bife asado steak' },
      { char: '🍗', name: 'Pollo', keywords: 'pollo presas broaster frito crispy' },
      { char: '🍖', name: 'Costilla', keywords: 'costilla rib carne' },
      { char: '🥘', name: 'Guiso / Paella', keywords: 'guiso cazuela sarten paella' },
      { char: '🍝', name: 'Pasta', keywords: 'pasta fideos espagueti spaguetti' },
      { char: '🍲', name: 'Sopa', keywords: 'sopa caldo plato caliente ramen' },
      { char: '🍤', name: 'Marisco', keywords: 'camaron marisco tempura langostino' },
      { char: '🍣', name: 'Sushi', keywords: 'sushi roll maki japones' },
      { char: '🍚', name: 'Arroz', keywords: 'arroz rice bowl guarnicion' },
    ],
  },
  {
    id: 'healthy',
    name: 'Saludable y Entradas',
    icon: '🥗',
    emojis: [
      { char: '🥗', name: 'Ensalada', keywords: 'ensalada verde salad verduras' },
      { char: '🥑', name: 'Aguacate', keywords: 'aguacate palta avocado guacamole' },
      { char: '🥟', name: 'Empanada / Dumpling', keywords: 'empanada dumpling gyoza dim sum' },
      { char: '🧀', name: 'Queso', keywords: 'queso cheese tabla aperitivo' },
      { char: '🥐', name: 'Croissant', keywords: 'croissant medialuna panaderia' },
      { char: '🥖', name: 'Pan / Baguette', keywords: 'pan baguette panaderia bread' },
    ],
  },
  {
    id: 'desserts',
    name: 'Postres y Dulces',
    icon: '🍰',
    emojis: [
      { char: '🍰', name: 'Pastel / Torta', keywords: 'pastel torta pastelito cake postre' },
      { char: '🍦', name: 'Helado', keywords: 'helado cono cream ice' },
      { char: '🍩', name: 'Dona', keywords: 'dona donut rosquilla' },
      { char: '🍪', name: 'Galleta', keywords: 'galleta cookie dulce' },
      { char: '🍫', name: 'Chocolate', keywords: 'chocolate bombon barra' },
      { char: '🥞', name: 'Panqueques', keywords: 'panqueques pancakes hotcakes waffle' },
    ],
  },
  {
    id: 'drinks',
    name: 'Bebidas y Cafetería',
    icon: '🍹',
    emojis: [
      { char: '☕', name: 'Café', keywords: 'cafe coffee espresso te tea' },
      { char: '🧃', name: 'Jugo', keywords: 'jugo zumo box juice refresco' },
      { char: '🥤', name: 'Refresco / Soda', keywords: 'soda refresco gaseosa vaso cup' },
      { char: '🍺', name: 'Cerveza', keywords: 'cerveza beer chop schop' },
      { char: '🍷', name: 'Vino', keywords: 'vino wine copa tinto' },
      { char: '🍸', name: 'Cóctel', keywords: 'coctel martini trago cocktail' },
      { char: '🧋', name: 'Boba / Té', keywords: 'boba bubble tea batido smoothie' },
    ],
  },
  {
    id: 'utensils',
    name: 'Utensilios y Menaje',
    icon: '🍽️',
    emojis: [
      { char: '🍽️', name: 'Menú / Plato', keywords: 'plato cubiertos tenedor cuchillo menu' },
      { char: '🥢', name: 'Palillos', keywords: 'palillos chopsticks oriental' },
      { char: '🥣', name: 'Bowl / Tazón', keywords: 'bowl tazon sopa cereal' },
      { char: '🍳', name: 'Sartén / Huevo', keywords: 'sarten huevo frito desayuno cooking' },
      { char: '🧂', name: 'Sal / Especias', keywords: 'sal especia salero condimento' },
    ],
  },
];

@Component({
  selector: 'app-emoji-picker',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './emoji-picker.component.html',
})
export class EmojiPickerComponent {
  readonly selectedEmoji = input<string>('🍽️');
  readonly disabled = input<boolean>(false);
  readonly emojiSelected = output<string>();

  readonly isOpen = signal<boolean>(false);
  readonly searchQuery = signal<string>('');
  readonly activeSectionId = signal<string>('all');

  readonly sections = EMOJI_SECTIONS;

  readonly filteredEmojis = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const sectionId = this.activeSectionId();

    const pool: EmojiItem[] =
      sectionId === 'all'
        ? this.sections.flatMap((s) => s.emojis)
        : (this.sections.find((s) => s.id === sectionId)?.emojis ?? []);

    if (!query) {
      return pool;
    }

    return pool.filter(
      (item) =>
        item.char.includes(query) ||
        item.name.toLowerCase().includes(query) ||
        item.keywords.toLowerCase().includes(query),
    );
  });

  toggle(): void {
    if (this.disabled()) return;
    this.isOpen.update((v) => !v);
    if (!this.isOpen()) {
      this.resetFilter();
    }
  }

  open(): void {
    if (this.disabled()) return;
    this.isOpen.set(true);
  }

  close(): void {
    this.isOpen.set(false);
    this.resetFilter();
  }

  selectEmoji(emoji: string): void {
    this.emojiSelected.emit(emoji);
    this.close();
  }

  selectSection(id: string): void {
    this.activeSectionId.set(id);
  }

  onCustomEmojiInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const value = input.value?.trim();
    if (value) {
      this.selectEmoji(value);
      input.value = '';
    }
  }

  private resetFilter(): void {
    this.searchQuery.set('');
    this.activeSectionId.set('all');
  }
}
