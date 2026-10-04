import { useState } from 'react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useCategories } from '@/features/category/hooks'
import {
  useCreateCategory,
  useDeleteCategory,
  useUpdateCategory,
} from '@/features/admin/categoriesHooks'
import type { CategoryDTO } from '@/api/types'

function CategoryRow({ category }: { category: CategoryDTO }) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(category.categoryName ?? '')
  const updateMutation = useUpdateCategory()
  const deleteMutation = useDeleteCategory()

  if (editing) {
    return (
      <div className="flex flex-wrap items-center gap-2 rounded-xl border border-border p-4">
        <Input
          value={name}
          onChange={(e) => setName(e.target.value)}
          aria-label="Edit category name"
          className="h-8 flex-1"
        />
        <Button
          size="sm"
          disabled={name.trim().length < 5 || updateMutation.isPending}
          onClick={() =>
            category.categoryId &&
            updateMutation.mutate(
              { categoryId: category.categoryId, payload: { categoryName: name } },
              { onSuccess: () => setEditing(false) }
            )
          }
        >
          Save
        </Button>
        <Button size="sm" variant="outline" onClick={() => setEditing(false)}>
          Cancel
        </Button>
      </div>
    )
  }

  return (
    <div className="flex items-center justify-between gap-2 rounded-xl border border-border p-4">
      <span className="text-sm font-medium">{category.categoryName}</span>
      <div className="flex gap-2">
        <Button size="sm" variant="outline" onClick={() => setEditing(true)}>
          Edit
        </Button>
        <Button
          size="sm"
          variant="ghost"
          className="text-destructive"
          disabled={deleteMutation.isPending}
          onClick={() => category.categoryId && deleteMutation.mutate(category.categoryId)}
        >
          Delete
        </Button>
      </div>
    </div>
  )
}

export function AdminCategoriesPage() {
  const [newName, setNewName] = useState('')
  const { data, isPending, isError } = useCategories()
  const createMutation = useCreateCategory()

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-xl font-semibold">Categories</h1>

      <div className="flex flex-wrap items-end gap-2 rounded-xl border border-border p-4">
        <div className="flex flex-1 flex-col gap-1">
          <label className="text-xs text-muted-foreground">New category name</label>
          <Input
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            aria-label="New category name"
            className="h-8"
          />
        </div>
        <Button
          size="sm"
          disabled={newName.trim().length < 5 || createMutation.isPending}
          onClick={() =>
            createMutation.mutate({ categoryName: newName }, { onSuccess: () => setNewName('') })
          }
        >
          Add category
        </Button>
      </div>

      {isPending && (
        <div className="flex flex-col gap-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full" />
          ))}
        </div>
      )}

      {isError && (
        <p className="text-sm text-destructive">Couldn't load categories. Please try again.</p>
      )}

      {data && (data.content?.length ?? 0) === 0 && (
        <p className="text-sm text-muted-foreground">No categories yet.</p>
      )}

      {data && (data.content?.length ?? 0) > 0 && (
        <div className="flex flex-col gap-3">
          {data.content!.map((category) => (
            <CategoryRow key={category.categoryId} category={category} />
          ))}
        </div>
      )}
    </div>
  )
}
